package override;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component (role=Override.class)
public class OverrideImpl implements Override {

  @Requirement(hint="other")
  private Override r01;

}
