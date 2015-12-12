package cn.paxos.araneae;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.paxos.jam.StateContext;
import cn.paxos.jam.event.BytesEvent;
import cn.paxos.jam.preset.json.state.InitState;
import cn.paxos.jam.state.BytesState;
import cn.paxos.pocket.Gadget;
import cn.paxos.pocket.Pocket;
import cn.paxos.pocket.btree.BytesWrapper;

public class AutohomeTest
{
  
  private static final Logger log = LoggerFactory.getLogger(AutohomeTest.class);

  private static final Pocket configPocket = new Pocket("/var/autohome/pocket/config");

  @Test
  public void test() throws InterruptedException
  {
    Context context = new Context("http://car.autohome.com.cn/AsLeftMenu/As_LeftListNew.ashx?typeId=1%20&brandId=0%20&fctId=0%20&seriesId=0");
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
        return page.getUrl().indexOf("/AsLeftMenu/") > -1 && page.getUrl().indexOf("brandId=0") > -1;
      }
      @Override
      public void handle(Context context, Page page, String content)
      {
        Pattern p = Pattern.compile("<a href='/price/brand-([\\d]+).html'><[^>]+></i>([^<]+)");
        Matcher m = p.matcher(content);
        while (m.find())
        {
          String brand = m.group(1);
          String name = m.group(2);
          log.trace("list " + brand + " " + name);
          String url = "http://car.autohome.com.cn/AsLeftMenu/As_LeftListNew.ashx?typeId=1%20&brandId=" + brand + "%20&fctId=0%20&seriesId=0";
          System.out.println("sublist = " + url);
          Page sublistPage = new Page(page, url);
          sublistPage.setAttachment(name);
          sublistPage.fetch(context);
        }
      }
    });
    context.addHandler(new Handler()
    {
      @Override
      public boolean accept(Page page)
      {
        return page.getUrl().indexOf("/AsLeftMenu/") > -1 && page.getUrl().indexOf("brandId=0") < 0;
      }
      @Override
      public void handle(Context context, Page page, String content)
      {
        Pattern p = Pattern.compile(" href='(/price/series-[\\d]+.html)'>([^<]+)");
        Matcher m = p.matcher(content);
        while (m.find())
        {
          String link = m.group(1);
          String name = m.group(2).trim();
          log.trace("sublist " + page.getAttachment() + " " + link + " " + name);
          String url = "http://car.autohome.com.cn" + link;
          System.out.println("series = " + url);
          Page seriesPage = new Page(page, url);
          seriesPage.setAttachment(page.getAttachment() + " " + name);
          try
          {
            seriesPage.fetch(context);
          } catch (Exception e)
          {
            log.warn("warn cache " + url, e);
          }
        }
      }
    });
    context.addHandler(new Handler()
    {
      @Override
      public boolean accept(Page page)
      {
        return page.getUrl().indexOf("/price/series") > -1;
      }
      @Override
      public void handle(Context context, Page page, String content)
      {
        if (content.indexOf("<title>Object moved</title>") > -1)
        {
          content = content.substring(content.indexOf("\"") + 1);
          String moved = content.substring(0, content.indexOf("\""));
          String url = "http://car.autohome.com.cn" + moved;
          System.out.println("moved = " + url);
          Page seriesPage = new Page(page, url);
          seriesPage.setAttachment(page.getAttachment());
          seriesPage.fetch(context);
          return;
        }
        String subBrand = extractSubBrand(content);
        log.trace("subBrand " + page.getAttachment() + " " + subBrand);
        content = content.substring(content.indexOf("\"divSeries\""));
        content = content.substring(0, content.indexOf("</ul>"));
        try
        {
          content = content.substring(content.indexOf("\"interval01-list-cars-text\""));
        } catch (Exception e)
        {
          log.warn("warn divSeries " + page.getAttachment() + " " + subBrand + "\r\n[[[" + content + "]]]", e);
          return;
        }
        content = content.substring(content.indexOf(">") + 1);
        String powerCategory = content.substring(0, content.indexOf("<"));
        log.trace("powerCategory " + powerCategory);
        while (content.indexOf("<a href=") > -1)
        {
          content = content.substring(content.indexOf("<a href=") + 1);
          content = content.substring(content.indexOf(">") + 1);
          String spec = content.substring(0, content.indexOf("<"));
          log.trace("spec " + page.getAttachment() + " " + subBrand + " " + spec);
          content = content.substring(content.indexOf("\"interval01-list-guidance\""));
          content = content.substring(content.indexOf("</a>") + 4);
          String guidance = content.substring(0, content.indexOf("<")).trim();
          log.trace("guidance " + guidance);
          try
          {
            content = content.substring(content.indexOf("<a href=\"/config/spec/"));
          } catch (Exception e)
          {
            log.warn("warn spec " + page.getAttachment() + " " + subBrand + "\r\n[[[" + content + "]]]", e);
            return;
          }
          content = content.substring(content.indexOf("\"") + 1);
          String configHref = content.substring(0, content.indexOf("\""));
          log.trace("configHref " + page.getAttachment() + " " + subBrand + " " + spec + " " + configHref);
          String url = "http://car.autohome.com.cn" + configHref;
          if (url.indexOf('#') > -1)
          {
            url = url.substring(0, url.indexOf('#'));
          }
          System.out.println("config = " + url);
          Page seriesPage = new Page(page, url);
          seriesPage.setAttachment(spec);
          seriesPage.fetch(context);
        }
      }
      private String extractSubBrand(String content)
      {
        content = content.substring(content.indexOf("\"breadnav\""));
        content = content.substring(0, content.indexOf("</div>"));
        content = content.substring(content.indexOf("</a>") + 4);
        content = content.substring(content.indexOf("</a>") + 4);
        content = content.substring(content.indexOf("</a>") + 4);
        content = content.substring(content.indexOf(">") + 1);
        content = content.substring(0, content.indexOf("</a>"));
        return content;
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
          BytesWrapper key = new BytesWrapper();
          key.append(id);
          Gadget gadget = new Gadget(key, true);
          Map<String, String> attrs = map.get(id);
          for (String attrName : attrs.keySet())
          {
            String attrValue = attrs.get(attrName);
            gadget.setAttribute(attrName, attrValue);
            System.out.println(attrName + " = " + attrValue);
          }
          configPocket.put(gadget);
          System.out.println("saved " + id);
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
        stateContext.publish(new BytesEvent(jsonLine.getBytes(Charset.forName("GBK"))));
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
              String specid = (String) valueitem.get("specid");
              String value = (String) valueitem.get("value");
              put(map, specid, "/config/" + paramtypeitemName + "/" + paramitemName, value);
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
              String specid = (String) valueitem.get("specid");
              String value = (String) valueitem.get("value");
              put(map, specid, "/option/" + configtypeitemName + "/" + configitemName, value);
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
    Thread.currentThread().join();
  }

}
