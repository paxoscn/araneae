package cn.paxos.araneae;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

public class Context
{
  
  public static final int BUFFER_SIZE = 16 * 1024;
  private static final int threadPoolSize = Runtime.getRuntime().availableProcessors();
  public static final AsynchronousChannelGroup asynchronousChannelGroup;
  
  static
  {
    try
    {
      asynchronousChannelGroup = AsynchronousChannelGroup.withCachedThreadPool(Executors.newCachedThreadPool(), threadPoolSize);
    } catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private final String startURL;
  private final List<Configer> configers;
  private final List<Handler> handlers;

  public Context(String startURL)
  {
    this.startURL = startURL;
    this.configers = new LinkedList<Configer>();
    this.handlers = new LinkedList<Handler>();
  }
  
  public void start()
  {
    Page startPage = new Page(null, startURL);
    startPage.fetch(this);
  }

  public Config config(String url)
  {
    Config config = new Config();
    for (Configer configer : configers)
    {
      if (configer.accept(url))
      {
        configer.update(config); 
      }
    }
    return config;
  }

  public void handle(Config config, Page page)
  {
    String content = new String(page.getContent().getBytes(), page.getContent().getOffset(), page.getContent().getLength(), config.getCharset());
    for (Handler handler : handlers)
    {
      if (handler.accept(page))
      {
        handler.handle(this, page, content); 
      }
    }
  }
  
  public void addConfiger(Configer configer)
  {
    configers.add(configer);
  }
  
  public void addHandler(Handler handler)
  {
    handlers.add(handler);
  }

}
