package webapps.service;

import webapps.cache.RemoteAccountCache;
import webapps.guice.EntityRegistrarModule;
import webapps.service.model.LocalAccountProviderImpl;
import biz.turnonline.ecosystem.steward.facade.AccountStewardAdapterModule;
import biz.turnonline.ecosystem.steward.facade.AccountStewardApiModule;
import biz.turnonline.ecosystem.steward.model.Account;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.ctoolkit.restapi.client.appengine.CtoolkitRestFacadeAppEngineModule;
import org.ctoolkit.restapi.client.appengine.DefaultOrikaMapperFactoryModule;
import org.ctoolkit.restapi.client.provider.LocalResourceProvider;
import org.ctoolkit.restapi.client.pubsub.PubsubMessageListener;
import org.ctoolkit.services.storage.guice.GuicefiedOfyFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * The application injection configuration.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class MicroserviceModule
        extends AbstractModule
{
    @Override
    protected void configure()
    {
        install( new EntityRegistrarModule() );
        install( new CtoolkitRestFacadeAppEngineModule() );
        install( new DefaultOrikaMapperFactoryModule() );
        install( new AccountStewardApiModule() );
        install( new AccountStewardAdapterModule() );

        bind( GuicefiedOfyFactory.class ).asEagerSingleton();
        bind( LocalAccountProvider.class ).to( LocalAccountProviderImpl.class );

        // will be interpreted by REST Facade
        bind( new TypeLiteral<LocalResourceProvider<Account>>()
        {
        } ).to( RemoteAccountCache.class );
    }

    @Provides
    @Singleton
    ObjectMapper provideJsonObjectMapper()
    {
        JsonFactory factory = new JsonFactory();
        factory.enable( JsonParser.Feature.ALLOW_COMMENTS );

        SimpleModule module = new SimpleModule();
        module.addSerializer( Long.class, new JsonLongSerializer() );

        ObjectMapper mapper = new ObjectMapper( factory );
        mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        mapper.registerModule( module );
        mapper.setDateFormat( new SimpleDateFormat( PubsubMessageListener.PUB_SUB_DATE_FORMAT ) );

        return mapper;
    }

    /**
     * The {@link Long} value published via Google Endpoints is being serialized as {@link String}
     * in order to be compatible with JavaScript. To make Google Endpoints Client and its model
     * compatible with Google Pub/Sub we need to serialize published messages
     * with {@link Long} as {@link String} as well.
     */
    private static class JsonLongSerializer
            extends JsonSerializer<Long>
    {
        @Override
        public void serialize( Long value, JsonGenerator generator, SerializerProvider serializers ) throws IOException
        {
            generator.writeString( value.toString() );
        }
    }
}
