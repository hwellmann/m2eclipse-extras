package simple.p001;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

@Component (role=P001Simple.class)
public class P001SimpleImpl implements P001Simple {

  @Requirement
  @Configuration(value="so-so")
  private P001Simple r01;

}
