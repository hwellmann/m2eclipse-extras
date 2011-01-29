package simple.p003;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

import simple.p002.*;

@Component (role=P002Simple.class, hint="03")
public class P003Simple01Impl extends P002Simple01Impl implements P002Simple {

  @Requirement
  private P002Simple r02;

}
