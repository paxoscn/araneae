package cn.paxos.araneae;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

import cn.paxos.pocket.Gadget;
import cn.paxos.pocket.Pocket;
import cn.paxos.pocket.btree.BytesWrapper;

public class AutohomeSave
{

  private static final String logFile = "/var/autohome/trace.log";

  private static final Pocket configPocket = new Pocket("/var/autohome/pocket/config");

  @Test
  public void test() throws InterruptedException, IOException
  {
    int c = 0;
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
    for (String line; (line = br.readLine()) != null; )
    {
      if (!line.startsWith("attr "))
      {
        continue;
      }
      line = line.substring(5);
      String id = line.substring(0, line.indexOf(' '));
      line = line.substring(line.indexOf(' ') + 1);
      String attrName = line.substring(0, line.indexOf(" === "));
      line = line.substring(line.indexOf(" === ") + 5);
      String attrValue = line;
      BytesWrapper key = new BytesWrapper();
      key.append(id);
      Gadget gadget = configPocket.get(key);
      if (gadget == null)
      {
        gadget = new Gadget(key, true);
      }
      gadget.setAttribute(attrName, attrValue);
      configPocket.put(gadget);
      if ((++c) % 1000 == 0)
      {
        System.out.println(c);
      }
    }
    br.close();
  }

}
