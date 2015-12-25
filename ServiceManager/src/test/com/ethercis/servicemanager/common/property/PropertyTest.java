//Copyright
package com.ethercis.servicemanager.common.property;

import junit.framework.TestCase;

public class PropertyTest extends TestCase {

    /**
     * For testing only
     * <p />
     * <pre>
     * java -Djava.compiler= org.xmlBlaster.common.property.Property -Persistence.Driver myDriver -isCool true -xml "<hello>world</hello>"
     * java -Djava.compiler= org.xmlBlaster.common.property.Property -dump true -uu "You are the user \${user.name}" -double "\${uu} using \${java.vm.name}"
     * java -Djava.compiler= org.xmlBlaster.common.property.Property -NameList Josua,David,Ken,Abel
     * java -Djava.compiler= org.xmlBlaster.common.property.Property -urlVariant true -url http://localhost/xy.properties
     * java -Djava.compiler= org.xmlBlaster.common.property.Property -hashVariant true
     * java -Djava.compiler= org.xmlBlaster.common.property.Property -dump true -val[A] aaaa -val[B] bbbb -val[C][1] cccc -val[C][2] c2c2
     * </pre>
     */
    public void testProperty() {
//  try {
//      boolean dump = Args.getArg(args, "-dump", false);
//      if (dump) {
//         Property props = new Property("jutils-test.properties", true, args, true); // initialize
//         System.out.println(props.toXml());
//      }
//   } catch (Exception e) {
//      System.err.println(e.toString());
//   }
//
//   try {
//     boolean testClasspathProperties = Args.getArg(args, "-testClasspathProperties", false);
//     if (testClasspathProperties) {
//        Property props = new Property("jutils-test.properties", true, args, true); // initialize
//        System.out.println("data=" + props.get("data", "ERROR"));
//        props.saveProps();
//     }
//   } catch (Exception e) {
//     System.err.println(e.toString());
//   }
//
//   try {
//     boolean testSetArray = Args.getArg(args, "-testSetArray", false);
//     if (testSetArray) {
//        Property props = new Property(null, true, args, true); // initialize
//        props.set("Array[a][b]", "arrayvalue");
//        System.out.println(props.toXml());
//        System.exit(0);
//     }
//   } catch (Exception e) {
//     System.err.println(e.toString());
//   }
//
//   try {
//     boolean urlVariant = Args.getArg(args, "-urlVariant", false);
//     if (urlVariant) {
//       {
//         System.out.println("*** Test 1");
//         Property props = new Property(null, true, (java.applet.Applet)null, true); // initialize
//         System.out.println("TestVariable=" + props.get("TestVariable", "ERROR"));
//         System.out.println("java.home=" + props.get("java.home", "ERROR"));
//       }
//       {
//         System.out.println("*** Test 2");
//         String url = Args.getArg(args, "-url", "http://localhost/xy.properties");
//         Property props = new Property(url, true, (java.applet.Applet)null, true); // initialize
//         System.out.println("TestVariable=" + props.get("TestVariable", "ERROR"));
//         System.out.println("java.home=" + props.get("java.home", "ERROR"));
//       }
//       System.exit(0);
//     }
//   } catch (ServiceManagerException e) {
//     //System.err.println(e.toXml(true));
//     System.err.println(e.toString());
//     System.exit(0);
//   }
//
//   try {
//     boolean hashVariant = Args.getArg(args, "-hashVariant", false);
//     if (hashVariant) {
//       {
//         System.out.println("*** Test 1");
//         Properties extra = new Properties();
//         extra.put("city", "Auckland");
//         extra.put("country", "NewZealand");
//         Property props = new Property(null, true, extra, true); // initialize
//         System.out.println("city=" + props.get("city", "ERROR"));
//         System.out.println("country=" + props.get("country", "ERROR"));
//         System.out.println("TestVariable=" + props.get("TestVariable", "shouldBeUndef"));
//         System.out.println("java.home=" + props.get("java.home", "ERROR"));
//       }
//       System.exit(0);
//     }
//   } catch (ServiceManagerException e) {
//     //System.err.println(e.toXml(true));
//     System.err.println(e.toString());
//     System.exit(0);
//   }
    }



}