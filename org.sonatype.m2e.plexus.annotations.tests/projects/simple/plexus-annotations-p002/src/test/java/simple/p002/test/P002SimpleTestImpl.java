package simple.p002.test;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

import simple.p002.*;

@Component (role=P002Simple.class, hint="Test")
public class P002SimpleTestImpl extends P002Simple01Impl implements P002Simple {

  @Requirement
  private P002Simple rTest;

}
