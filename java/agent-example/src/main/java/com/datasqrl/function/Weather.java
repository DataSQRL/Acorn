package com.datasqrl.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class Weather {

  @JsonPropertyDescription("City and state, for example: León, Guanajuato")
  public String location;

  @JsonPropertyDescription("The temperature unit, can be 'celsius' or 'fahrenheit'")
  @JsonProperty(required = true)
  public WeatherUnit unit;

  public enum WeatherUnit {
    CELSIUS, FAHRENHEIT;
  }

  public static class WeatherResponse {
    public String location;
    public WeatherUnit unit;
    public int temperature;
    public String description;

    public WeatherResponse(String location, WeatherUnit unit, int temperature, String description) {
      this.location = location;
      this.unit = unit;
      this.temperature = temperature;
      this.description = description;
    }
  }

}
