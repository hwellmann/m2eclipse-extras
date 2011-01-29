package simple.p004;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

@Component (role=P004Simple.class)
public class P004SimpleImpl implements P004Simple {

  @Requirement(hint="other")
  private P004Simple r01;

}
