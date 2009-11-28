/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.kdb;

import studio.core.DefaultAuthenticationMechanism;
import java.io.*;
import java.util.*;
import java.util.List;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.awt.*;

public class Config
{
    public static String imageBase="/de/skelton/images/";
    public static String imageBase2="/de/skelton/utils/";
    private static String path;
    private static String filename="studio.properties";
    private static String absoluteFilename;
    private static String version="1.1";
    private Properties p = null;
    private static Config instance;
    private static NumberFormat formatter= null;

    private Config()
    {
        init();
    }

    public Font getFont()
    {
        Font f= null;

        if( p != null)
        {
            String name=    p.getProperty("font.name");
            String size=    p.getProperty("font.size");
            int s= 14;
            if( size != null)
                s= Integer.parseInt(size);
            String n= "Monospaced";
            if( name != null)
                n= name;

            f= new Font(n, Font.PLAIN, s);

            if( f == null)
                f= new Font("Monospaced", Font.PLAIN, 14);
            
            setFont(f);
        }

        return f;
    }

    public void setFont(Font f)
    {
        if( p != null)
        {
            p.setProperty("font.name", f.getFamily());
            p.setProperty("font.size", ""+f.getSize());
            save();
        }
    }
    
    public Color getColorForToken(String tokenType, Color defaultColor)
    {
        Color c= Color.black;

        if( p != null)
        {
            String s= p.getProperty("token."+tokenType);
            if(s != null){
                c= new Color(Integer.parseInt(s.substring(0,2),16),
                        Integer.parseInt(s.substring(2,4),16),
                        Integer.parseInt(s.substring(4,6),16));
            }
            else {
                c= defaultColor;
                setColorForToken(tokenType,c);
            }
        }

        return c;
    }

    public void setColorForToken(String tokenType, Color c)
    {
        if( p != null)
        {
            p.setProperty("token."+tokenType, Integer.toHexString(c.getRGB()).substring(2));
            save();
        }
    }
    
    public synchronized NumberFormat getNumberFormat()
    {
        String key= null;

        if( p != null)
        {
            key= p.getProperty( "DecimalFormat","#.#######");
        }

        return new DecimalFormat(key);
    }

    public static Config getInstance()
    {
        if (instance == null)
        {
            instance = new Config();
        }

        return instance;
    }

    private void init()
    {
        path= System.getProperties().getProperty( "user.home");

        path= path + "/.studioforkdb";

        File f= new File( path);

        if( ! f.exists())
        {
            if( !f.mkdir())
            {
                // error creating dir
            }
        }

        absoluteFilename= path + "/" + filename;

        String candidate= absoluteFilename;

        p = new Properties();

        boolean finished= false;

        while( !finished)
        {
            FileInputStream in = null;

            try
            {
                in = new FileInputStream(candidate);

                try
                {
                    p.load(in);
                    String v= p.getProperty( "version");
                    if( (v == null) || (!version.equals( v)))
                    {
                        p.clear();
                    }
                }
                catch (IOException e)
                {
                }
                finally
                {
                    finished= true;
                }
            }
            catch (FileNotFoundException e)
            {
                if( candidate.equals( absoluteFilename))
                {
                    candidate= filename;
                }
                else {
                    finished= true;
                }
            }
            finally
            {
                try
                {
                    if (in != null)
                    {
                        in.close();
                    }
                }
                catch (IOException e)
                {
                }
            }
        }
    }


    public void save()
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(absoluteFilename);
            try
            {
                p.put( "version", version);
                p.store(out, "Auto-generated by Studio for kdb+");
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
        finally
        {
            try
            {
                if (out != null)
                {
                    out.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            }
        }
    }

    public String[] getQKeywords()
    {
        String key= null;

        if( p != null)
        {
            key= p.getProperty( "qkeywords");
        }

        Vector keywords= new Vector();

        if( key != null)
        {
            StringTokenizer t= new StringTokenizer( key, ",");
            while( t.hasMoreTokens())
            {
                String token= t.nextToken().trim();

                if( token.length() > 0)
                {
                    if( ! keywords.contains( token))
                    {
                        keywords.add(token);
                    }
                }
            }
        }

        return (String []) keywords.toArray( new String[0]);
    }

    public String getLRUServer()
    {
        String key= null;

        if( p != null)
        {
            key= p.getProperty( "lruServer");
        }

        return key;
    }

    public void setLRUServer(Server s)
    {
        if( s != null)
        {
            if( p != null)
            {
                p.put("lruServer", s.getName());
            }

            save();
        }
    }


    public void saveQKeywords( String [] keywords)
    {
        StringBuffer key= new StringBuffer();

        for( int i=0; i < keywords.length; i++)
        {
            if( i > 0)
            {
                key.append( ",");
            }

            key.append( keywords[i].trim());
        }

        if( p != null)
        {
            p.put("qkeywords", key.toString());
        }

        save();
    }
    
    public void setAcceptedLicense(Date d)
    {
        p.put("licenseAccepted", d.toString());
        save();
    }

    public boolean getAcceptedLicense()
    {
        String s=(String) p.get("licenseAccepted");
        if(s == null)
            return false;
        if( s.length()==0)
            return false;
        
        if(Lm.buildDate.after(new Date(s)))
            return false;
        
        return true;
    }
    
    public int getOffset( Server server)
    {
        if( server != null)
        {
            String name= server.getName();
            Server [] servers= getServers();

            for( int i= 0; i < servers.length; i++)
            {
                if( name.equals( servers[i].getName()))
                {
                    return i;
                }
            }
        }

        return -1;
    }

    public String [] getMRUFiles()
    {
        String mru= null;

        if( p != null)
        {
            mru= p.getProperty( "mrufiles");
        }

        Vector mruFiles= new Vector();

        if( mru != null)
        {
            StringTokenizer t= new StringTokenizer( mru, ",");
            while( t.hasMoreTokens())
            {
                String token= t.nextToken().trim();

                if( token.length() > 0)
                {
                    if( ! mruFiles.contains( token))
                    {
                        mruFiles.add(token);
                    }
                }
            }
        }

        return (String []) mruFiles.toArray( new String[0]);
    }


    public void saveMRUFiles( String [] mruFiles)
    {
        StringBuffer mru= new StringBuffer();

        for( int i=0; i < (mruFiles.length>9?9:mruFiles.length); i++)
        {
            if( i > 0)
            {
                mru.append( ",");
            }

            mru.append( mruFiles[i].trim());
        }

        if( p != null)
        {
            p.put("mrufiles", mru.toString());
        }

        save();
    }

    public String getLookAndFeel()
    {
        String lf= null;

        if( p != null)
        {
            lf= p.getProperty( "lookandfeel");
        }

        return lf;
    }

    public void setLookAndFeel( String lf)
    {
        if( p != null)
        {
            p.put("lookandfeel", lf);
        }

        save();
    }

    public Server getServer( String server)
    {
        Server [] servers= getServers();
        for( int i= 0; i < servers.length; i++)
        {
            if( server.equals( servers[i].getName()))
            {
                return servers[i];
            }
        }

        return null;
    }

    public String[] getServerNames()
    {
        Server [] servers= getServers();
        String [] names= new String[ servers.length];
        for( int i=0;i<servers.length;i++)
        {
            names[i]=servers[i].getName();
        }

        return names;
    }

    public Server[] getServers()
    {
        ArrayList list = new ArrayList();

        String servers = p.getProperty("Servers");

        if (servers != null)
        {
            StringTokenizer t = new StringTokenizer(servers, ",");

            while (t.hasMoreTokens())
            {
                String name = t.nextToken().trim();

                String host = p.getProperty( "server."+name + "." + "host");
                int port = Integer.parseInt( p.getProperty( "server."+name + "." + "port", "-1"));
                String username = p.getProperty( "server."+name + "." + "user");
                String password = p.getProperty( "server."+name + "." + "password");
                String backgroundColor= p.getProperty( "server."+name + "." + "backgroundColor", "FFFFFF");
                String authenticationMechanism = p.getProperty( "server."+name + "." + "authenticationMechanism",new DefaultAuthenticationMechanism().getMechanismName());

                Color c= new Color(Integer.parseInt(backgroundColor.substring(0,2),16),
                        Integer.parseInt(backgroundColor.substring(2,4),16),
                        Integer.parseInt(backgroundColor.substring(4,6),16));
                if( (host != null) || ( port > 0))
                {
                    Server server = new Server(name, host, port, username, password,c, authenticationMechanism);
                    list.add(server);
                }
            }
        }

        return (Server[]) list.toArray(new Server[0]);
    }

    public void removeServer(Server server)
    {
        Server [] servers=getServers();

        ArrayList l= new ArrayList();
        for( int i= 0; i < servers.length; i ++)
        {
            if( ! server.getName().equals( servers[i].getName()))
            {
                l.add( servers[i]);
            }
        }

        p.remove( "server."+server.getName()+"."+"host");
        p.remove( "server."+server.getName()+"."+"port");
        p.remove( "server."+server.getName()+"."+"k4");
        p.remove( "server."+server.getName()+"."+"user");
        p.remove( "server."+server.getName()+"."+"password");
        p.remove( "server."+server.getName()+ "." + "backgroundColor");
        p.remove( "server."+server.getName()+ "." + "authenticationMechanism");

        setServers( (Server []) l.toArray( new Server[0]));
    }

    public void saveServer(Server server)
    {
        Server [] servers=getServers();

        ArrayList l= new ArrayList();
        for( int i= 0; i < servers.length; i ++)
        {
            if( ! server.getName().equals( servers[i].getName()))
            {
                l.add( servers[i]);
            }
            else {
                l.add( server);
            }
        }

        setServers( (Server []) l.toArray( new Server[0]));
    }

    private void setServerDetails( Server server)
    {
        String name = server.getName();

        p.setProperty( "server."+name + "." + "host", server.getHost());
        p.setProperty( "server."+name + "." + "port", "" + server.getPort());
        p.setProperty( "server."+name + "." + "user", "" + server.getUsername());
        p.setProperty( "server."+name + "." + "password", "" + server.getPassword());
        p.setProperty( "server."+name + "." + "backgroundColor", "" + Integer.toHexString(server.getBackgroundColor().getRGB()).substring(2));
        p.setProperty( "server."+name + "." + "authenticationMechanism", server.getAuthenticationMechanism());
    }

    public void addServer(Server server)
    {
        setServerDetails( server);

        List serverNames= new ArrayList();

        String servers = p.getProperty("Servers", "");

        boolean found = false;

        StringTokenizer t = new StringTokenizer(servers, ",");

        while (t.hasMoreTokens())
        {
            String name = t.nextToken().trim();
            serverNames.add( name);
        }

        if( !serverNames.contains( server.getName()))
        {
            serverNames.add( server.getName());
            Collections.sort( serverNames);

            Iterator i= serverNames.iterator();

            StringBuffer s= new StringBuffer();

            while( i.hasNext())
            {
                s.append( (String) i.next());
                if( i.hasNext())
                {
                    s.append( ",");
                }
            }

            p.setProperty("Servers", s.toString());
        }

        save();
    }

    public void setServers(Server[] servers)
    {
        String names = "";

        for (int i = 0; i < servers.length; i++)
        {
            setServerDetails( servers[i]);

            if (i > 0)
            {
                names += ",";
            }

            names += servers[i].getName().trim();
        }

        p.setProperty("Servers", names);

        save();
    }
}
