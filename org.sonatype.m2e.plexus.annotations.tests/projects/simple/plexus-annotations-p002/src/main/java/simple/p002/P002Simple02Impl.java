package simple.p002;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

@Component (role=P002Simple.class, hint="02")
public class P002Simple02Impl extends P002Simple01Impl implements P002Simple {

  @Requirement
  private P002Simple r02;

}
