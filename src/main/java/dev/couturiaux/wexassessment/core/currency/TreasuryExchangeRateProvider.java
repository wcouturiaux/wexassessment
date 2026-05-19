package dev.couturiaux.wexassessment.core.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class TreasuryExchangeRateProvider {

  private final ObjectMapper objectMapper;
  private Map<String, String> mappings = new HashMap<>();

  @Value("classpath:treasury-mappings.json")
  private Resource mappingFile;

  public TreasuryExchangeRateProvider(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  public void loadMappings() {
    try (InputStream inputStream = mappingFile.getInputStream()) {
      Map<String, String> rawMap = objectMapper.readValue(inputStream, new TypeReference<>() {});
      this.mappings = Map.copyOf(rawMap);
    } catch (Exception e) {
      throw new IllegalStateException("App deployment aborted.");
    }
  }

  public Optional<String> getTreasuryDescription(String countryCurrencyKey) {
    if (countryCurrencyKey == null || countryCurrencyKey.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(mappings.get(countryCurrencyKey));
  }
}
