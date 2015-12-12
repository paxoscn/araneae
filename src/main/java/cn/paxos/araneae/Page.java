package cn.paxos.araneae;

import java.nio.channels.CompletionHandler;

import cn.paxos.araneae.net.Client;
import cn.paxos.jam.util.BytesWithOffset;
import cn.paxos.pocket.Gadget;
import cn.paxos.pocket.Pocket;
import cn.paxos.pocket.btree.BytesWrapper;

public class Page
{

  private static final Pocket cachePocket = new Pocket("/var/autohome/pocket/cache");
  
  private final String url;
  private final Page parent;

  private BytesWithOffset content;
  private Object attachment;

  public Page(Page parent, String url)
  {
    this.parent = parent;
    this.url = url;
    this.content = null;
    this.attachment = null;
  }

  public void fetch(final Context context)
  {
    final Config config = context.config(url);
    BytesWrapper key = new BytesWrapper();
    key.append(url);
    Gadget cachedGadget;
    try
    {
      cachedGadget = cachePocket.get(key);
      if (cachedGadget != null)
      {
        context.handle(config, Page.this, cachedGadget.getAttribute("content"));
        return;
      }
    } catch (Exception e)
    {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }
    System.out.println("visiting " + url);
    Client.visit(url, config, new CompletionHandler<BytesWithOffset, Void>() {
      @Override
      public void completed(BytesWithOffset body, Void attachment)
      {
        content = body;
        context.handle(config, Page.this);
      }
      @Override
      public void failed(Throwable exc, Void attachment)
      {
        // TODO
        exc.printStackTrace();
      }
    });
  }

  public void cache(String content)
  {
    BytesWrapper key = new BytesWrapper();
    key.append(url);
    Gadget cachedGadget = new Gadget(key, true);
    cachedGadget.setAttribute("content", content);
    cachePocket.put(cachedGadget);
  }

  public String getUrl()
  {
    return url;
  }

  public Page getParent()
  {
    return parent;
  }

  public BytesWithOffset getContent()
  {
    return content;
  }

  public Object getAttachment()
  {
    return attachment;
  }

  public void setAttachment(Object attachment)
  {
    this.attachment = attachment;
  }

}
