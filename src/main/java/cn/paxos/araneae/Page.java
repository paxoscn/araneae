package cn.paxos.araneae;

import java.nio.channels.CompletionHandler;

import cn.paxos.araneae.net.Client;
import cn.paxos.jam.util.BytesWithOffset;

public class Page
{
  
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
