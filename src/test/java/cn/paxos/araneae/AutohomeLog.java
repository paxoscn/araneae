package cn.paxos.araneae;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.paxos.jam.StateContext;
import cn.paxos.jam.event.BytesEvent;
import cn.paxos.jam.preset.json.state.InitState;
import cn.paxos.jam.state.BytesState;
import cn.paxos.pocket.Gadget;
import cn.paxos.pocket.btree.BytesWrapper;

public class AutohomeLog
{
  
  private static final Logger log = LoggerFactory.getLogger(AutohomeLog.class);

  private static final String logFile = "/var/autohome/trace.log";

//  private static final Pocket configPocket = new Pocket("/var/autohome/pocket/config");

  @Test
  public void test() throws InterruptedException, IOException
  {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
    for (String line; (line = br.readLine()) != null; )
    {
      if (line.startsWith("configHref "))
      {
        String url = "http://car.autohome.com.cn" + line.substring(line.lastIndexOf(" ") + 1);
        if (url.indexOf('#') > -1)
        {
          url = url.substring(0, url.indexOf('#'));
        }
        System.out.println(url);
        Context context = new Context(url);
        context.addConfiger(new Configer()
        {
          @Override
          public boolean accept(String url)
          {
            return true;
          }
          @Override
          public void update(Config config)
          {
            config.setCharset(Charset.forName("GBK"));
          }
        });
        context.addHandler(new Handler()
        {
          @Override
          public boolean accept(Page page)
          {
            return page.getUrl().indexOf("/config/spec") > -1;
          }
          @Override
          public void handle(Context context, Page page, String content)
          {
            if (content.indexOf("var keyLink = ") < 0)
            {
              log.warn("warn keyLink " + content);
              return;
            }
            content = content.substring(content.indexOf("var keyLink = "));
            content = content.substring(0, content.indexOf("var dealerPrices = "));
            StringTokenizer st = new StringTokenizer(content, "\n");
            /*List<Object> keyLinks = deserializeJson(*/st.nextToken()/*)*/;
            List<Object> configs = deserializeJson(st.nextToken());
            List<Object> options = deserializeJson(st.nextToken());
            /*List<Object> colors = deserializeJson(st.nextToken());
            List<Object> innerColors = deserializeJson(st.nextToken());
            List<Object> bags = deserializeJson(st.nextToken());*/
            Map<String, Map<String, String>> map = map(configs, options);
            for (String id : map.keySet())
            {
              System.out.println("saving " + id);
              log.trace("saving " + id);
              BytesWrapper key = new BytesWrapper();
              key.append(id);
              Gadget gadget = new Gadget(key, true);
              Map<String, String> attrs = map.get(id);
              for (String attrName : attrs.keySet())
              {
                String attrValue = attrs.get(attrName);
                gadget.setAttribute(attrName, attrValue);
                log.trace("attr " + id + " " + attrName + " === " + attrValue);
              }
//              configPocket.put(gadget);
            }
          }
          @SuppressWarnings("unchecked")
          private List<Object> deserializeJson(String jsonLine)
          {
            jsonLine = jsonLine.trim();
            jsonLine = jsonLine.substring(jsonLine.indexOf("["));
            jsonLine = jsonLine.substring(0, jsonLine.lastIndexOf("]") + 1);
            StateContext stateContext = new StateContext();
            stateContext.start();
            stateContext.addState(new BytesState());
            InitState initState = new InitState();
            stateContext.addState(initState);
            stateContext.publish(new BytesEvent(jsonLine.getBytes(Charset.forName("UTF-8"))));
            return (List<Object>) initState.getContainer().toCollection(false);
          }
          @SuppressWarnings("unchecked")
          private Map<String, Map<String, String>> map(
              List<Object> configs,
              List<Object> options)
          {
            Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
            for (Object paramtypeitemObject : configs)
            {
              Map<String, Object> paramtypeitem = (Map<String, Object>) paramtypeitemObject;
              String paramtypeitemName = (String) paramtypeitem.get("name");
              List<Object> paramitems = (List<Object>) paramtypeitem.get("paramitems");
              for (Object paramitemObject : paramitems)
              {
                Map<String, Object> paramitem = (Map<String, Object>) paramitemObject;
                String paramitemName = (String) paramitem.get("name");
                List<Object> valueitems = (List<Object>) paramitem.get("valueitems");
                for (Object valueitemObject : valueitems)
                {
                  Map<String, Object> valueitem = (Map<String, Object>) valueitemObject;
                  String specid = ((Integer) valueitem.get("specid")).toString();
                  String value = (String) valueitem.get("value");
                  put(map, specid, "///config///" + paramtypeitemName + "///" + paramitemName, value);
                }
              }
            }
            for (Object configtypeitemObject : options)
            {
              Map<String, Object> configtypeitem = (Map<String, Object>) configtypeitemObject;
              String configtypeitemName = (String) configtypeitem.get("name");
              List<Object> configitems = (List<Object>) configtypeitem.get("configitems");
              for (Object configitemObject : configitems)
              {
                Map<String, Object> configitem = (Map<String, Object>) configitemObject;
                String configitemName = (String) configitem.get("name");
                List<Object> valueitems = (List<Object>) configitem.get("valueitems");
                for (Object valueitemObject : valueitems)
                {
                  Map<String, Object> valueitem = (Map<String, Object>) valueitemObject;
                  String specid = ((Integer) valueitem.get("specid")).toString();
                  String value = (String) valueitem.get("value");
                  put(map, specid, "///option///" + configtypeitemName + "///" + configitemName, value);
                }
              }
            }
            return map;
          }
          private void put(
              Map<String, Map<String, String>> map,
              String specid,
              String attrName,
              String attrValue)
          {
            Map<String, String> spec = map.get(specid);
            if (spec == null)
            {
              spec = new HashMap<String, String>();
              map.put(specid, spec);
            }
            spec.put(attrName, attrValue);
          }
        });
        try
        {
          context.start();
        } catch (Exception e)
        {
          log.warn("warn start", e);
        }
      }
    }
    br.close();
    Thread.currentThread().join();
  }

}
