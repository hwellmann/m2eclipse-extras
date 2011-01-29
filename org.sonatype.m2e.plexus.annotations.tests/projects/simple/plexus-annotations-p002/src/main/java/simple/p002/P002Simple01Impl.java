package simple.p002;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

public class P002Simple01Impl implements P002Simple {

  @Requirement
  private P002Simple r01;

}
