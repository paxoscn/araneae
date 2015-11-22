package cn.paxos.araneae;

public interface Handler
{

  boolean accept(Page page);

  void handle(Context context, Page page, String content);

}
