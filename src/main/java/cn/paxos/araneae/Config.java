package cn.paxos.araneae;

import java.nio.charset.Charset;

public class Config
{

  private int interval = 10000;
  private Charset charset = Charset.forName("UTF-8");

  public int getInterval()
  {
    return interval;
  }

  public void setInterval(int interval)
  {
    this.interval = interval;
  }

  public Charset getCharset()
  {
    return charset;
  }

  public void setCharset(Charset charset)
  {
    this.charset = charset;
  }

}
