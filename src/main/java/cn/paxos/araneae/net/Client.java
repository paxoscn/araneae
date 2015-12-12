package cn.paxos.araneae.net;

import java.nio.channels.CompletionHandler;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.paxos.araneae.Config;
import cn.paxos.araneae.Context;
import cn.paxos.jam.preset.http.client.HTTPClient;
import cn.paxos.jam.util.BytesWithOffset;

public class Client
{
  
  private static final int threadCount = Runtime.getRuntime().availableProcessors();
  private static final DaemonThread[] threads = new DaemonThread[threadCount];
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
  private static final Map<String, Long> lastVisitedMap = new ConcurrentHashMap<String, Long>();
  
  static
  {
    for (int i = 0; i < threads.length; i++)
    {
      DaemonThread thread = new DaemonThread();
      threads[i] = thread;
      threadPool.submit(thread);
    }
  }

  public static void visit(String url, Config config, CompletionHandler<BytesWithOffset, Void> completionHandler)
  {
    threads[(int) (Math.random() * threadCount)].offer(new Visit(url, config, completionHandler));
  }
  
  private static class DaemonThread implements Runnable
  {
    
    // TODO Disrupter
    private final Queue<Visit> queue = new LinkedList<Visit>();

    @Override
    public void run()
    {
      while (true)
      {
        synchronized (queue)
        {
          for (Iterator<Visit> iterator = queue.iterator(); iterator.hasNext();)
          {
            Visit visit = iterator.next();
            if (canVisit(visit.getUrl(), visit.getConfig()))
            {
              doVisit(visit.getUrl(), visit.getConfig(), visit.getCompletionHandler());
              iterator.remove();
            }
          }
        }
        try
        {
          Thread.sleep(3000);
        } catch (InterruptedException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    private boolean canVisit(String url, Config config)
    {
      String host = host(url);
      Long lastVisited = lastVisitedMap.get(host);
      if (lastVisited == null)
      {
        lastVisited = 0L;
      }
//      System.out.println(System.currentTimeMillis() + " " + lastVisited + " " + host);
      return System.currentTimeMillis() - lastVisited > config.getInterval();
    }

    private void doVisit(String url, Config config,
        final CompletionHandler<BytesWithOffset, Void> completionHandler)
    {
      final String host = host(url);
//      System.out.println(System.currentTimeMillis() + " " + host);
      lastVisitedMap.put(host, System.currentTimeMillis());
      url = url.substring(url.indexOf("://") + 3);
      String path = "/";
      int indexOfSlash = url.indexOf('/');
      if (indexOfSlash > -1)
      {
        path = url.substring(indexOfSlash);
        url = url.substring(0, indexOfSlash);
      }
      int port = 80;
      int indexOfColon = url.indexOf(':');
      if (indexOfColon > -1)
      {
        port = Integer.parseInt(url.substring(indexOfColon + 1));
      }
      String reqStr = "GET " + path + " HTTP/1.1\r\nAccept-Encoding: gzip,deflate,sdch\r\nHost: " + host + ":" + port + "\r\nConnection: close\r\nCache-Control: no-cache\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\nPragma: no-cache\r\nUser-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36\r\n\r\n";
      HTTPClient.visit(
          Context.BUFFER_SIZE,
          Context.asynchronousChannelGroup,
          host,
          port,
          reqStr,
          new CompletionHandler<BytesWithOffset, Void>() {
            @Override
            public void completed(BytesWithOffset body, Void attachment)
            {
              completionHandler.completed(body, attachment);
            }
            @Override
            public void failed(Throwable exc, Void attachment)
            {
              completionHandler.failed(exc, attachment);
            }
          });
    }

    private String host(String url)
    {
      String host = url.substring(url.indexOf("://") + 3);
      int indexOfSlash = host.indexOf('/');
      if (indexOfSlash > -1)
      {
        host = host.substring(0, indexOfSlash);
      }
      int indexOfColon = host.indexOf(':');
      if (indexOfColon > -1)
      {
        host = host.substring(0, indexOfColon);
      }
      return host;
    }

    public void offer(Visit visit)
    {
      synchronized (queue)
      {
        queue.offer(visit);
      }
    }
    
  }
  
  private static class Visit
  {
    
    private final String url;
    private final Config config;
    private final CompletionHandler<BytesWithOffset, Void> completionHandler;
    
    public Visit(String url, Config config,
        CompletionHandler<BytesWithOffset, Void> completionHandler)
    {
      this.url = url;
      this.config = config;
      this.completionHandler = completionHandler;
    }

    public String getUrl()
    {
      return url;
    }
    public Config getConfig()
    {
      return config;
    }
    public CompletionHandler<BytesWithOffset, Void> getCompletionHandler()
    {
      return completionHandler;
    }
    
  }

}
