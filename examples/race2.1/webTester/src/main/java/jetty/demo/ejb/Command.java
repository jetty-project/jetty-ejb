package jetty.demo.ejb;

import javax.servlet.http.HttpServletResponse;

public interface Command
{
    void exec(HttpServletResponse resp, String arg) throws Throwable;

    String getName();
}
