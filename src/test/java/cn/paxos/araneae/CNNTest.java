package cn.paxos.araneae;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import cn.paxos.jam.StateContext;
import cn.paxos.jam.event.BytesEvent;
import cn.paxos.jam.preset.json.state.InitState;
import cn.paxos.jam.state.BytesState;

public class CNNTest
{

  private final String folder = "/var/cnn/";
  
  private int pageNum = 1;

  @Test
  public void test() throws InterruptedException
  {
    Context context = new Context(url(pageNum));
    context.addHandler(new Handler()
    {
      @Override
      public boolean accept(Page page)
      {
        return page.getUrl().indexOf("/search/") > -1;
      }
      @SuppressWarnings("unchecked")
      @Override
      public void handle(Context context, Page page, String content)
      {
//        System.out.println(content);
        String json = content.substring(0, content.lastIndexOf("}") + 1);
        StateContext stateContext = new StateContext();
        stateContext.start();
        stateContext.addState(new BytesState());
        InitState initState = new InitState();
        stateContext.addState(initState);
        stateContext.publish(new BytesEvent(json.getBytes(Charset.forName("UTF-8"))));
        Map<String, Object> map = (Map<String, Object>) initState.getContainer().toCollection(true);
        List<Object> results = (List<Object>) ((List<Object>) map.get("results")).get(0);
        for (Object e : results)
        {
          Map<String, Object> result = (Map<String, Object>) e;
          String title = (String) result.get("title");
          System.out.println("title = " + title);
          String description = (String) result.get("description");
          System.out.println("description = " + description);
          String collection = (String) result.get("collection");
          String url = (String) result.get("url");
          System.out.println("url = " + url);
          if (collection.equals("Stories"))
          {
            if (!url.startsWith("http://"))
            {
              url = "http://edition.cnn.com" + url;
            }
            System.out.println("next url = " + url);
            if (!existsFile(url))
            {
              System.out.println("skipped " + url);
              Page storyPage = new Page(page, url);
              storyPage.setAttachment(result);
              storyPage.fetch(context);
            }
          }
        }
        pageNum++;
        Page storyPage = new Page(null, url(pageNum));
        storyPage.fetch(context);
      }
    });
    context.addHandler(new Handler()
    {
      @Override
      public boolean accept(Page page)
      {
        return page.getUrl().indexOf("edition.cnn.com") > -1;
      }
      @Override
      public void handle(Context context, Page page, String content)
      {
        if (existsFile(page.getUrl()))
        {
          System.out.println("skipped " + page.getUrl());
        } else
        {
          System.out.println("saving " + page.getUrl());
          savePage(page);
        }
      }
    });
    context.start();
    Thread.currentThread().join();
  }

  private String url(int pageNum)
  {
    return "http://searchapp.cnn.com/search/query.jsp?page=" + pageNum + "&npp=10&start=" + (pageNum * 10 + 1) + "&text=south%2Bchina%2Bsea&type=all&bucket=true&sort=relevance&csiID=csi2";
  }

  private boolean existsFile(String url)
  {
    File file = new File(folder + md5(url) + ".html");
    return file.exists();
  }

  private void savePage(Page page)
  {
    File file = new File(folder + md5(page.getUrl()) + ".html");
    try
    {
      FileOutputStream out = new FileOutputStream(file);
      @SuppressWarnings("unchecked")
      Map<String, Object> attrs = (Map<String, Object>) page.getAttachment();
      out.write((((String) attrs.get("url")) + "\r\n").getBytes());
      out.write((((String) attrs.get("collection")) + "\r\n").getBytes());
      out.write((((String) attrs.get("title")) + "\r\n").getBytes());
      out.write((((String) attrs.get("description")) + "\r\n\r\n").getBytes());
      out.write(page.getContent().getBytes(), page.getContent().getOffset(), page.getContent().getLength());
      out.close();
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }
  }
  
  private static String md5(String str)
  {
    // TODO init once ?
    try
    {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] bs = md.digest(str.getBytes());
      StringBuilder sb = new StringBuilder();
      for (byte b : bs)
      {
        String hex = Integer.toHexString(0xFF & b);
        if (hex.length() == 1)
        {
          sb.append('0');
        }
        sb.append(hex);
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
