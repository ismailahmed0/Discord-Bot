import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Hello, I will find weather and covid-19 information for you.
Please follow the provided format for expected output.
――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――
For weather, enter weather: City Name/ZIP Code
Example: weather: Richardson or weather: New York or weather: 75080
Note: For zip code, so long as you have 'weather' and 5 digits consecutively somewhere, it will accept it.
――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――
For covid-19 cases, enter covid: State Initial/world/country: Country Code Letters/County FIPS Code
Example: covid: TX or covid: world or covid: country: US or covid: 48085
Note 1: For state, so long as you have 'covid' and 2 upper letters consecutively somewhere, it will accept it.
Note 2: For world, so long as you have 'covid' and 'world' somewhere, it will accept it.
Note 3: For county, so long as you have 'covid' and 5 digits consecutively somewhere, it will accept it.
 */

public class Bot extends ListenerAdapter
{
    // class data fields
    int apiVal;
    String apiInfo;
    String endpoint;
    String jsonString;
    Format newFormat = new Format(); // create new object of Format class
    API myAPI = new API(); // create new object of API class
    Json json = new Json(); // create new object of Json class
    String cmdList = "Hello, I will find weather and covid-19 information for you.\n" +
            "Please follow the provided format for expected output.\n" +
            "―――――――――――――――――――――――――――――――――――――――――――――――――――――\n" +
            "For weather, enter !weather: City Name/ZIP Code\n" +
            "Example: !weather: Richardson or !weather: New York or !weather: 75080\n" +
            "Note 1: For zip code, so long as you have '!' at start and 'weather' and 5 digits consecutively somewhere, it will accept it.\n" +
            "―――――――――――――――――――――――――――――――――――――――――――――――――――――\n" +
            "For covid-19 cases, enter !covid: State Initial/world/country: Country Code Letters/County FIPS Code\n" +
            "Example: !covid: TX or !covid: world or !covid: country: US or !covid: 48085\n" +
            "Note 1: For state, so long as you have '!' at start and 'covid' and 2 upper letters consecutively somewhere, it will accept it.\n" +
            "Note 2: For world, so long as you have '!' at start and 'covid' and 'world' somewhere, it will accept it.\n" +
            "Note 3: For county, so long as you have '!' at start and 'covid' and 5 digits consecutively somewhere, it will accept it.";

    public static void main(String[] args) throws Exception
    {
        JDABuilder.createLight("ENTER YOUR TOKEN HERE", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new Bot()).setActivity(Activity.playing("Type !cmds")).build();

    }
    public void onMessageReceived(MessageReceivedEvent event)
    {
        Message msg = event.getMessage();
        if (msg.getContentRaw().charAt(0) == '!')
        {
            MessageChannel channel = event.getChannel();
            //channel.sendMessage("Pong!").queue();
            apiVal = newFormat.setFormat(msg.getContentRaw()); // set place and get which api to use
            if (apiVal > 0 && apiVal != 5) // if api found and apiVal not 5(cmd list request)
            {
                endpoint = myAPI.getAPI(newFormat.getPlace(), apiVal); // get api url

                if (!endpoint.equals("0"))
                {
                    jsonString = json.getJSON(endpoint); // get json string

                    if (!jsonString.equals("0"))
                    {
                        if (apiVal == 1) // if api weather
                        {
                            apiInfo = json.jsonWeather(jsonString, newFormat.getPlace());
                        }
                        else if (apiVal == 2) // if api covid state/county
                        {
                            apiInfo = json.jsonStateCountyCovid(jsonString, myAPI.getPlace());
                        }
                        else if (apiVal == 3) // if api covid world
                        {
                            apiInfo = json.jsonWorldCovid(jsonString);
                        }
                        else // if api covid country
                        {
                            apiInfo = json.jsonCountryCovid(jsonString);
                        }
                        channel.sendMessage(apiInfo).queue();

                        //sendMessage(channel, apiInfo); // send api info to channel
                    }
                    else
                    {
                        channel.sendMessage("Sorry, I was unable to retrieve the information for that.").queue();

                        //sendMessage(channel, "Sorry, I was unable to retrieve the information for that.");
                    }
                }
                else
                {
                    channel.sendMessage("Sorry, your input is not correct. Check the format and try again!").queue();

                    //sendMessage(channel, "Sorry, your input is not correct. Check the format and try again!");
                }
            }
            else if (apiVal == 5)
            {
                channel.sendMessage(cmdList).queue();
            }
            else
            {
                channel.sendMessage("Sorry, your input is not correct. Check the format and try again!").queue();

                //sendMessage(channel, "Sorry, your input is not correct. Check the format and try again!");
            }
        }
        //System.out.println(msg.getContentRaw());
    }
}

class Format
{
    // class data fields
    String sMessage = "";
    String place = "";
    String[] val;
    StringBuilder str = new StringBuilder("");
    Pattern regex = Pattern.compile("(\\d{5})"); // pattern of 5 digits consecutively
    Pattern regex2 = Pattern.compile("([A-Z]{2})"); // pattern of 2 upper case letters consecutively

    // mutator method
    void setPlace(String place)
    {
        this.place = place;
    }

    // accessor method
    String getPlace()
    {
        return place;
    }

    // method
    int setFormat(String message)
    {
        sMessage = message; // save for covid state initials
        message = message.toLowerCase();

        if (message.contains("weather") && (!message.contains("covid")))
        {
            val = message.split(" "); // split up weather and requested place into array
            if ((val.length == 2) && (val[0].equals("!weather:"))) // if format matches
            {
                setPlace(val[1]); // set place of city name
                return 1; // set api to weather
            }
            else if ((val.length > 2) && (val[0].equals("!weather:"))) // if city name has space(New York)
            {
                for (int i = 1; (i < val.length - 1); i++)
                {
                    str.append(val[i]); // %20 to represent space in url
                    str.append("%20");
                }
                str.append(val[val.length - 1]); // so there isn't %20 after last word
                setPlace(str.toString()); // set place of city name
                str.setLength(0); // clears the string for new use

                return 1; // set api to weather
            }
            else
            {
                Matcher match = regex.matcher(message);

                // if 5 digit consecutively in message
                if (match.find())
                {
                    setPlace(match.group(1)); // set place of zip code
                    return 1; // set api to weather
                }
                else
                {
                    return 0; // fail
                }
            }
        }
        else if (message.contains("covid") && (!message.contains("weather")))
        {
            val = message.split(" "); // split up covid and requested place into array
            if ((val.length == 2) && (val[0].equals("covid:"))) // if format matches
            {
                if ((val[1].equals("world")))
                {
                    setPlace(val[1]); // set place to world
                    return 3; // set api to covid world
                }
                else // if val[1] == state initial
                {
                    setPlace(val[1].toUpperCase()); // necessary for state initial to be uppercase for API access
                    return 2; // set api to covid state
                }
            }
            else if ((val.length == 3) && (val[0].equals("!covid:")) && (val[1].equals("country:")))
            {
                setPlace(val[2]); // set place to country
                return 4; // set api to covid country
            }
            else
            {
                Matcher match1 = regex.matcher(message); // for "regular" messages
                Matcher match2 = regex2.matcher(sMessage); // to find it if message also has 2 upper case letters

                // if 5 digit number consecutively in message
                if (match1.find())
                {
                    setPlace(match1.group(1)); // set place to county fips code
                    return 2; // set api to covid county
                }
                else if (match2.find()) // if 2 upper letters consecutively in message
                {
                    setPlace(match2.group(1)); // set place to state
                    return 2; // set api to covid state
                }
                else if (message.contains("world"))
                {
                    setPlace("world"); // set place to world
                    return 3; // set api to covid world
                }
                else
                {
                    return 0; // fail
                }
            }
        }
        else if (message.equals("!cmds"))
        {

            return 5; // !cmds list
        }
        else // if neither weather nor covid api, or both - only 1 request at a time
        {
            return 0; // fail
        }
    }
}

class API
{
    // class data fields
    String place = "";
    String apiKey = "";
    String endpoint = "";

    // mutator method
    void setPlace(String place)
    {
        this.place = place;
    }

    // accessor method
    String getPlace()
    {
        return place;
    }

    String getAPI(String place, int apiVal)
    {
        if (apiVal == 1) // if api weather
        {
            apiKey = "97d64ed46e8ecee86dbc9dc76278cfaf";

            if (place.contains("%20")) // to verify only %20 and letters in city name
            {
                String[] newStr = place.split("%20"); // split words into array from %20
                for (int i = 0; (i < newStr.length); i++)
                {
                    if (!(newStr[i].matches("[a-z]+"))) // if word doesn't only have letters
                    {
                        return "0";
                    }
                }
                endpoint = "http://api.openweathermap.org/data/2.5/weather?q="+place+"&units=imperial&appid="+apiKey;
            }
            else if (place.matches("[a-z]+")) // if place entered is city name
            {
                endpoint = "http://api.openweathermap.org/data/2.5/weather?q="+place+"&units=imperial&appid="+apiKey;
            }
            else if (place.matches("[0-9]+")) // if place entered is zip code
            {
                endpoint = "http://api.openweathermap.org/data/2.5/weather?zip="+place+"&units=imperial&appid="+apiKey;
            }
            else
            {
                return "0";
            }
        }
        else if (apiVal == 2) // if api covid US state or county
        {
            apiKey = "09592c356c7949d89ee0ce75bba80669";

            if (place.matches("[a-zA-Z]+")) // if place entered is state initial
            {
                endpoint = "https://api.covidactnow.org/v2/state/"+place+".json?apiKey="+apiKey;
                setPlace("state"); // distinguish as json parse function is same as county
            }
            else if (place.matches("[0-9]+")) // if place entered is county FIPS code
            {
                endpoint = "https://api.covidactnow.org/v2/county/"+place+".json?apiKey="+apiKey;
                setPlace("county"); // distinguish as json parse function is same as state
            }
            else
            {
                return "0";
            }
        }
        else if (apiVal == 3)// if api covid world
        {
            if (place.matches("[a-z]+"))
            {
                endpoint = "https://2019ncov.asia/api/cdr";
            }
            else
            {
                return "0";
            }
        }
        else if (apiVal == 4)// if api covid country
        {
            if (place.matches("[a-zA-Z]+"))
            {
                endpoint = "http://api.coronatracker.com/v3/stats/worldometer/country?countryCode=" + place;
            }
            else
            {
                return "0";
            }
        }

        return endpoint;
    }
}

class Json
{
    String getJSON(String endpoint)
    {
        // local variables
        StringBuilder jsonString;
        String lineValue;

        try
        {
            jsonString = new StringBuilder(); // object from StringBuilder class

            // get the API
            URL url = new URL(endpoint); // set url to accessible GET resource
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            // read data from API
            BufferedReader data = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while ((lineValue = data.readLine()) != null) // keep reading till last line
            {
                jsonString.append(lineValue);
            }
            // close to prevent potential memory leaks
            data.close();
            con.disconnect();

            return jsonString.toString();
        }
        catch(Exception e)
        {
            return "0";
        }
    }

    String jsonWeather(String jsonString, String place)
    {
        // local variables
        String weather;
        String placeName;
        double temperature;
        double tempLow;
        double tempHigh;
        double windSpeed;
        JsonObject weatherList;
        JsonObject mainList;
        JsonObject windList;

        JsonObject object = new JsonParser().parse(jsonString).getAsJsonObject(); // parse json string, convert to objet to pull

        // get objects under weather array
        weatherList = object.getAsJsonArray("weather").get(0).getAsJsonObject(); //convert array to object
        weather = weatherList.get("description").getAsString(); // element from weather list
        weather = weather.substring(0, 1).toUpperCase() + weather.substring(1); // capitalize word

        // get objects under main list
        mainList = object.getAsJsonObject("main");
        temperature = mainList.get("temp").getAsDouble(); // element from main list
        tempLow = mainList.get("temp_min").getAsDouble();
        tempHigh = mainList.get("temp_max").getAsDouble();

        // get objects under wind list
        windList = object.getAsJsonObject("wind");
        windSpeed = windList.get("speed").getAsDouble();

        placeName = object.get("name").getAsString(); // get city name

        if (place.matches("[0-9]+")) // if place zip code
        {
            return (weather + " in " + placeName + "(" + place + ") with a temperature of " + temperature + "°F, a high of " +
                    tempHigh + "°F, a low of "+ tempLow +"°F, and a wind speed of " + windSpeed + " mph.");
        }

        return (weather + " in " + placeName + " with a temperature of " + temperature + "°F, a high of " +
                tempHigh + "°F, a low of "+ tempLow +"°F, and a wind speed of " + windSpeed + " mph.");
    }

    String jsonStateCountyCovid(String jsonString, String place)
    {
        // local variables
        JsonObject actualsList;
        int cases;
        int deaths;
        String placeName;
        String fipsCode;
        String lastUpdatedDate;

        JsonObject object = new JsonParser().parse(jsonString).getAsJsonObject(); // parse json string, convert to objet to pull

        // get objects under actuals list
        actualsList = object.getAsJsonObject("actuals");
        cases = actualsList.get("cases").getAsInt();
        deaths = actualsList.get("deaths").getAsInt();

        placeName = object.get(place).getAsString(); // get place name
        lastUpdatedDate = object.get("lastUpdatedDate").getAsString(); // get last updated date

        if (place.equals("county")) // if place county
        {
            fipsCode = object.get("fips").getAsString(); // get fips code

            return ("For covid-19 in " + placeName + "(" + fipsCode + "), there have been " + cases + " cases and " +
                    deaths + " deaths as of " + lastUpdatedDate + ".");
        }

        return ("For covid-19 in " + placeName + ", there have been " + cases + " cases and " + deaths + " deaths as of " +
                lastUpdatedDate + ".");
    }

    String jsonWorldCovid(String jsonString)
    {
        // local variables
        JsonObject caseList;
        JsonObject deathList;
        int cases;
        int deaths;
        long lastUpdatedDate;
        Date last_updated;

        JsonObject object = new JsonParser().parse(jsonString).getAsJsonObject(); // parse json string, convert to objet to pull

        // get objects under results array
        caseList = object.getAsJsonArray("results").get(0).getAsJsonObject(); //convert array to object
        cases = caseList.get("confirmed").getAsInt(); // element from results list

        deathList = object.getAsJsonArray("results").get(1).getAsJsonObject(); //convert array to object
        deaths = deathList.get("deaths").getAsInt(); // element from results list

        lastUpdatedDate = object.get("last_updated").getAsLong(); // get last updated date, is in Javascript version of Unix time
        last_updated = new Date(lastUpdatedDate);

        return("For covid-19 in the world, there have been " + cases + " cases and " + deaths + " deaths as of " +
                last_updated + ".");
    }

    String jsonCountryCovid(String jsonString)
    {
        // local variables
        JsonArray covidList;
        int cases;
        int deaths;
        String countryName;
        String countryCode;
        String lastUpdatedDate;

        covidList = (JsonArray) new JsonParser().parse(jsonString); // parse json using gson

        // get values from JSON array
        countryCode = ((JsonObject)covidList.get(0)).get("countryCode").getAsString();
        countryName = ((JsonObject)covidList.get(0)).get("country").getAsString();
        cases = ((JsonObject)covidList.get(0)).get("totalConfirmed").getAsInt();
        deaths = ((JsonObject)covidList.get(0)).get("totalDeaths").getAsInt();
        lastUpdatedDate = ((JsonObject)covidList.get(0)).get("lastUpdated").getAsString();
        lastUpdatedDate = lastUpdatedDate.substring(0, lastUpdatedDate.indexOf("T"));

        return ("For covid-19 in " + countryName + "(" + countryCode + "), there have been " + cases + " cases and " +
                deaths + " deaths as of " + lastUpdatedDate + ".");
    }
}
