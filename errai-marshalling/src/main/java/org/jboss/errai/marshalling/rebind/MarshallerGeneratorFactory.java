package org.jboss.errai.marshalling.rebind;

import org.jboss.errai.bus.rebind.ScannerSingleton;
import org.jboss.errai.bus.server.annotations.ExposeEntity;
import org.jboss.errai.bus.server.annotations.Portable;
import org.jboss.errai.bus.server.service.metadata.MetaDataScanner;
import org.jboss.errai.codegen.framework.Context;
import org.jboss.errai.codegen.framework.Statement;
import org.jboss.errai.codegen.framework.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.framework.builder.ConstructorBlockBuilder;
import org.jboss.errai.codegen.framework.meta.MetaClass;
import org.jboss.errai.codegen.framework.meta.MetaClassFactory;
import org.jboss.errai.codegen.framework.util.EmptyStatement;
import org.jboss.errai.codegen.framework.util.Implementations;
import org.jboss.errai.codegen.framework.util.Stmt;
import org.jboss.errai.marshalling.client.api.ClientMarshaller;
import org.jboss.errai.marshalling.client.api.Marshaller;
import org.jboss.errai.marshalling.client.api.MarshallerFactory;
import org.jboss.errai.marshalling.rebind.api.MappingContext;
import org.jboss.errai.marshalling.rebind.util.MarshallingUtil;

import javax.enterprise.util.TypeLiteral;
import java.util.*;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class MarshallerGeneratorFactory {
  private static final String MARSHALLERS_VAR = "marshallers";
  private static final String DEFAULT_ENCODING_TYPE = "json";

  private MappingContext mappingContext;


  public void generate() {
    ClassStructureBuilder<?> classStructureBuilder = Implementations.implement(MarshallerFactory.class);

    Context classContext = classStructureBuilder.getClassDefinition().getContext();
    mappingContext = new MappingContext(classContext);

    loadMarshallers();

    MetaClass javaUtilMap = MetaClassFactory.get(
            new TypeLiteral<Map<String, Marshaller>>() {
            }
    );

    Implementations.autoInitializedField(classStructureBuilder, javaUtilMap, MARSHALLERS_VAR, HashMap.class);

    ConstructorBlockBuilder<?> constructor = classStructureBuilder.publicConstructor();

    for (Map.Entry<String, Class<? extends Marshaller>> entry : mappingContext.getAllMarshallers().entrySet()) {
      constructor.append(
              Stmt.create(classContext).declareVariable(entry.getValue())
                      .asFinal()
                      .named(MarshallingUtil.getVarName(entry.getKey()))
                      .initializeWith(Stmt.newObject(entry.getValue())));
    }

    generateMarshallers(constructor, classContext);

    System.out.println(classStructureBuilder.toJavaString());
  }


  private void generateMarshallers(ConstructorBlockBuilder<?> constructor, Context classContext) {
    MetaDataScanner scanner = MetaDataScanner.createInstance();

    Set<Class<?>> exposed = new HashSet<Class<?>>(scanner.getTypesAnnotatedWith(ExposeEntity.class));
    exposed.addAll(scanner.getTypesAnnotatedWith(Portable.class));

    for (Class<?> clazz : exposed) {
      constructor.append(Stmt.create(classContext).loadVariable(MARSHALLERS_VAR)
              .invoke("put", clazz.getName(), marshall(clazz)));
    }

    constructor.finish();
  }

  private Statement marshall(Class<?> cls) {
    return MappingStrategyFactory.createStrategy(mappingContext, cls).getMapper().getMarshaller();
  }

  private void loadMarshallers() {
    Set<Class<?>> marshallers =
            ScannerSingleton.getOrCreateInstance().getTypesAnnotatedWith(ClientMarshaller.class);

    for (Class<?> cls : marshallers) {
      if (Marshaller.class.isAssignableFrom(cls)) {
        try {
          Class<?> type = (Class<?>) Marshaller.class.getMethod("getTypeHandled").invoke(cls.newInstance());
          mappingContext.registerMarshaller(type.getName(), cls.asSubclass(Marshaller.class));
        }
        catch (Throwable t) {
          throw new RuntimeException("could not instantiate marshaller class: " + cls.getName(), t);
        }
      }

      else {
        throw new RuntimeException("class annotated with " + ClientMarshaller.class.getCanonicalName()
                + " does not implement " + Marshaller.class.getName());
      }
    }
  }
}