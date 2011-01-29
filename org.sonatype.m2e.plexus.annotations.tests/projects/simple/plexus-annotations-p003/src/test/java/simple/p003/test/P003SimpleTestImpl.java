package simple.p003.test;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

import simple.p002.*;
import simple.p003.*;

@Component (role=P002Simple.class, hint="03Test")
public class P003SimpleTestImpl extends P003Simple01Impl implements P002Simple {

  @Requirement
  private P002Simple rTest;

}
