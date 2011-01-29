package simple.p005;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

@Component (role=P005SimpleImpl.class,hint=P005SimpleImpl.HINT)
public class P005SimpleImpl {

  public static final String HINT = "foole\u0064";

}
