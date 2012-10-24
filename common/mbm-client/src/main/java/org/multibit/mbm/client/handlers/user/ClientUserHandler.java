package org.multibit.mbm.client.handlers.user;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import org.multibit.mbm.auth.Authority;
import org.multibit.mbm.client.HalHmacResourceFactory;
import org.multibit.mbm.client.handlers.BaseHandler;
import org.multibit.mbm.model.ClientUser;
import org.multibit.mbm.model.CustomerUser;

import java.util.Locale;
import java.util.Map;

/**
 * <p>Handler to provide the following to {@link org.multibit.mbm.client.PublicMerchantClient}:</p>
 * <ul>
 * <li>Construction of public single item requests</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class ClientUserHandler extends BaseHandler {

  /**
   * @param locale The locale providing i18n information
   */
  public ClientUserHandler(Locale locale) {
    super(locale);
  }

  /**
   * Register an anonymous user for the current session
   *
   * @return A matching user
   */
  public Optional<ClientUser> registerAnonymously() {

    // TODO Replace "magic string" with auto-discover based on link rel
    String path = String.format("/client/user/anonymous");

    String hal = HalHmacResourceFactory.INSTANCE
      .newClientResource(locale, path)
      .post(String.class);

    // Read the HAL
    ReadableResource rr = readHalRepresentation(hal);

    Map<String, Optional<Object>> properties = rr.getProperties();

    ClientUser clientUser = new ClientUser();
    String apiKey = (String) properties.get("api_key").get();
    String secretKey = (String) properties.get("secret_key").get();

    if ("".equals(apiKey) || "".equals(secretKey)) {
      return Optional.absent();
    }

    // Must assume that the registration was successful
    // Using the credentials later would mean failed authentication anyway
    clientUser.setApiKey(apiKey);
    clientUser.setSecretKey(secretKey);
    clientUser.setCachedAuthorities(new Authority[]{Authority.ROLE_PUBLIC});

    return Optional.of(clientUser);
  }

  /**
   * Retrieve the user's own profile
   *
   * @param clientUser The ClientUser containing the API access information
   *
   * @return A matching {@link org.multibit.mbm.model.PublicItem}
   */
  public Optional<CustomerUser> retrieveProfile(ClientUser clientUser) {

    // Sanity check

    // TODO Replace "magic string" with auto-discover based on link rel
    String path = String.format("/users");
    String hal = HalHmacResourceFactory.INSTANCE
      .newUserResource(locale, path, clientUser)
      .get(String.class);

    // Read the HAL
    ReadableResource rr = readHalRepresentation(hal);

    Map<String, Optional<Object>> properties = rr.getProperties();

    CustomerUser customerUser = new CustomerUser();
    // Mandatory properties (will cause IllegalStateException if not present)
    customerUser.setSKU((String) properties.get("sku").get());
    // Optional direct properties
    if (properties.containsKey("gtin")) {
      Optional<Object> gtin = properties.get("gtin");
      if (gtin.isPresent()) {
        customerUser.setGTIN((String) gtin.get());
      }
    }
    // Optional properties
    for (Map.Entry<String, Optional<Object>> entry : properties.entrySet()) {
      customerUser.getOptionalProperties().put(entry.getKey(), (String) entry.getValue().get());
    }

    return Optional.of(customerUser);
  }

}