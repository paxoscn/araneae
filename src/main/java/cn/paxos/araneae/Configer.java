package cn.paxos.araneae;

public interface Configer
{

  boolean accept(String url);

  void update(Config config);

}
