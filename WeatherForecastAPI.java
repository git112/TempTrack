import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;

public class WeatherForecastAPI {

    private static final String FILE_NAME = "searched_cities.txt";
    private static HashSet<String> searchedCities = new HashSet<>();

    public static void main(String[] args) {
        loadSearchedCitiesFromFile();

        Scanner scanner = new Scanner(System.in);
        String city;

        do {
            System.out.println("=========================================");
            System.out.print("Enter City (Say No to Quit): ");
            city = scanner.nextLine();

            if (city.equalsIgnoreCase("No")) {
                break;
            }

            // Save the searched city
            searchedCities.add(city);
            saveCityToFile(city);

            // Get location data (latitude and longitude) using city name
            String locationData = getLocationData(city);
            if (locationData == null) {
                System.out.println("Error: Could not retrieve location data.");
                continue;
            }

            String[] location = locationData.split(",");
            double latitude = Double.parseDouble(location[0]);
            double longitude = Double.parseDouble(location[1]);

            // Get weather forecast using latitude and longitude
            getWeatherForecast(latitude, longitude);

        } while (!city.equalsIgnoreCase("No"));

        scanner.close();
        printSearchedCities();
    }

    // Load searched cities from a file
    private static void loadSearchedCitiesFromFile() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    searchedCities.add(line);
                }
                System.out.println("Loaded searched cities from file.");
            } catch (IOException e) {
                System.out.println("Error reading searched cities file.");
                e.printStackTrace();
            }
        } else {
            System.out.println("No previous search history found.");
        }
    }

    // Save city to file
    private static void saveCityToFile(String city) {
        try (FileWriter writer = new FileWriter(FILE_NAME, true)) {
            writer.write(city + "\n");
        } catch (IOException e) {
            System.out.println("Error saving city to file.");
            e.printStackTrace();
        }
    }

    // Print searched cities
    private static void printSearchedCities() {
        System.out.println("Cities you have searched for:");
        for (String city : searchedCities) {
            System.out.println(city);
        }
    }

    private static String getLocationData(String city) {
        city = city.replaceAll(" ", "+");
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + city + "&count=1&language=en&format=json";

        try {
            HttpURLConnection apiConnection = fetchApiResponse(urlString);
            if (apiConnection == null || apiConnection.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to the location API.");
                return null;
            }

            String jsonResponse = readApiResponse(apiConnection);

            if (jsonResponse.contains("\"latitude\":") && jsonResponse.contains("\"longitude\":")) {
                String latitude = jsonResponse.split("\"latitude\":")[1].split(",")[0];
                String longitude = jsonResponse.split("\"longitude\":")[1].split(",")[0];
                return latitude + "," + longitude;
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void getWeatherForecast(double latitude, double longitude) {
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                    "&longitude=" + longitude + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum&timezone=auto";

            HttpURLConnection apiConnection = fetchApiResponse(url);
            if (apiConnection == null || apiConnection.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to the weather API.");
                return;
            }

            String jsonResponse = readApiResponse(apiConnection);

            if (jsonResponse.contains("\"daily\":{")) {
                System.out.println("Weather Forecast for Latitude: " + latitude + ", Longitude: " + longitude);
                String dailyForecast = jsonResponse.split("\"daily\":\\{")[1].split("\\},\"daily_units\"")[0];

                String maxTemp = dailyForecast.split("\"temperature_2m_max\":\\[")[1].split("\\]")[0];
                String minTemp = dailyForecast.split("\"temperature_2m_min\":\\[")[1].split("\\]")[0];
                String precipitation = dailyForecast.split("\"precipitation_sum\":\\[")[1].split("\\]")[0];

                System.out.println("Max Temperatures for the next few days: " + maxTemp);
                System.out.println("Min Temperatures for the next few days: " + minTemp);
                System.out.println("Precipitation for the next few days: " + precipitation);
            } else {
                System.out.println("Error: No forecast data found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readApiResponse(HttpURLConnection apiConnection) {
        try {
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(apiConnection.getInputStream());

            while (scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }

            scanner.close();
            return resultJson.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
