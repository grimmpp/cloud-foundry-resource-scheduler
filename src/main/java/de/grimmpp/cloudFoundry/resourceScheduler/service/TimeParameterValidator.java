package de.grimmpp.cloudFoundry.resourceScheduler.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
public class TimeParameterValidator {

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    public static final String DEFAULT_VALUE = "8h";

    public static final boolean doesNotContainOrValidTimeParameter(Map<String,Object> map) {
        return !containsTimeParameter(map) || validateParameterValue(map);
    }

    /**
     * Checks if parameters contain one of the parameters.
     * @param parameters
     * @return
     */
    public static final boolean containsTimeParameter(Map<String,Object> parameters) {
        return  getContainedTimeParameter(parameters) != null;
    }

    public static final String getContainedTimeParameter(Map<String,Object> parameters) {
        if (parameters.containsKey(Parameter.KEY_FIXED_DELAY)) return Parameter.KEY_FIXED_DELAY;
        else if (parameters.containsKey(Parameter.KEY_TIMES)) return Parameter.KEY_TIMES;
        else return null;
    }

    public static final Parameter getTimeParameter(CreateServiceInstanceRequest request) {
        String key = getContainedTimeParameter(request.getParameters());

        Parameter p = Parameter.builder()
                .reference(request.getServiceInstanceId())
                .key(key)
                .build();

        if (key.equals(Parameter.KEY_FIXED_DELAY)) {
            p.setValue(request.getParameters().get(key).toString());
        } else if (key.equals(Parameter.KEY_TIMES)) {
            try {
                p.setValue(objectMapper.writeValueAsString(request.getParameters().get(key)));
            } catch (Throwable e) {
                throw new RuntimeException("Was not able to read parameter times.", e);
            }
        }

        return p;
    }

    public static final String getContainedTimeParameter(List<Parameter> parameters) {
        if (parameters.stream().anyMatch(p -> p.getKey().equals(Parameter.KEY_FIXED_DELAY))) return Parameter.KEY_FIXED_DELAY;
        else if (parameters.stream().anyMatch(p -> p.getKey().equals(Parameter.KEY_TIMES))) return Parameter.KEY_TIMES;
        else return null;
    }

    public static final boolean validateParameterValue(Map<String,Object> parameters) {
        String key = getContainedTimeParameter(parameters);
        if (key == null) return false;

        long countTimeParameters = parameters.keySet().stream().filter(
                k -> k.equals(Parameter.KEY_TIMES) || k.equals(Parameter.KEY_FIXED_DELAY)).count();
        if (countTimeParameters != 1) return false;

        if(Parameter.KEY_FIXED_DELAY.equals(key)) {
            return validateFixedDelayParameterValue(parameters.get(key).toString());
        } else if (Parameter.KEY_TIMES.equals(key)) {
            try {
                return validateTimesParameterValue((String[])parameters.get(key));
            } catch (Throwable e) {
                return false;
            }
        } else {
            return  false;
        }
    }

    public static final String getParameterFixedDelay(Map<String,Object> parameters, String defaultTime) {
        if (!containsTimeParameter(parameters)) return defaultTime;
        if (!validateParameterValue(parameters)) throw new RuntimeException();
        return formattingAndCleaningOfFixedDelay(parameters.get(Parameter.KEY_FIXED_DELAY).toString());
    }

    public static final String getParameterFixedDelay(CreateServiceInstanceRequest request, String defaultTime) {
        return getParameterFixedDelay(request.getParameters(), defaultTime);
    }

    public static final String getParameterFixedDelay(CreateServiceInstanceBindingRequest request, String defaultTime) {
        return getParameterFixedDelay(request.getParameters(), defaultTime);
    }

    public static final long getFixedDelayInMilliSecFromParameterValue(Map<String,Object> parameters, String defaultTime) {
        String time = defaultTime;
        if (containsTimeParameter(parameters)) time = parameters.get(Parameter.KEY_FIXED_DELAY).toString();
        return getFixedDelayInMilliSecFromParameterValue(time);
    }

    public static final boolean validateFixedDelayParameterValue(String parameterValue) {
        long time;

        try {
            time = getFixedDelayInMilliSecFromParameterValue(parameterValue);
        } catch (Throwable e) {
            log.error("Error in validating time parameter.", e);
            return  false;
        }
        return time > 0;
    }

    /**
     * Checks if parameter values are valid
     * @param parameterValues must be an array of times like ["14:53", "1:23"]
     * @return
     */
    public static final boolean validateTimesParameterValue(String[] parameterValues) {
        if (parameterValues == null) return false;
        if (parameterValues.length == 0) return  false;
        for (String time: parameterValues) {
            if (time.length() > 5 || time.length() < 3) return false;
            if (!time.contains(":")) return false;
            if (time.indexOf(':') != time.lastIndexOf(':')) return false;
            int hours = Integer.valueOf(time.split(":")[0]);
            int minutes = Integer.valueOf(time.split(":")[1]);
            if (hours < 0 || hours > 24) return false;
            if (minutes < 0 || minutes >= 60) return false;
            if (hours == 24 && minutes > 0) return false;
        }
        return true;
    }

    /**
     *
     * @param parameterValue is e.g. "1w 3d 5h 2m 23s"
     * @return
     */
    public static final long getFixedDelayInMilliSecFromParameterValue(String parameterValue) {
        long time = 0;

        List<String> timeSections = Arrays.asList(formattingAndCleaningOfFixedDelay(parameterValue).split(" "));
        for (String s : timeSections) time += getTimeSection(s);

        return time;
    }

    public static final long getFixedDelayInMilliSecFromParameterValue(List<Parameter> parameters) {
        return getFixedDelayInMilliSecFromParameterValue(Parameter.getParameterValueByKey(parameters, Parameter.KEY_FIXED_DELAY));
    }

    public static final long getFixedDelayInSecFromParameterValue(String parameterValue) {
        return getFixedDelayInMilliSecFromParameterValue(parameterValue) / 1000;
    }

    public static boolean isExpired(List<Parameter> parameters, long uptimeInSec) throws IOException {
        String key = getContainedTimeParameter(parameters);
        if (Parameter.KEY_TIMES.equals(key)) {
            return isTimesExpired(parameters);
        } else if (Parameter.KEY_FIXED_DELAY.equals(key)) {
            return isFixedDelayExpired(parameters, uptimeInSec);
        }
        return false;
    }

    public static boolean isFixedDelayExpired(List<Parameter> parameters, long uptimeInSec) {
        String time = Parameter.getParameterValueByKey(parameters, Parameter.KEY_FIXED_DELAY);
        long fixedDeplayInSec = getFixedDelayInSecFromParameterValue(time);
        return uptimeInSec > fixedDeplayInSec;
    }

    public static boolean isTimesExpired(List<Parameter> parameters) throws IOException {
        String[] times = objectMapper.readValue(Parameter.getParameterValueByKey(parameters, Parameter.KEY_TIMES), String[].class);
        for (String timeStr: times) {
            int hour = Integer.valueOf(timeStr.split(":")[0]);
            int min = Integer.valueOf(timeStr.split(":")[1]);
            long lastCall = Long.valueOf( Parameter.getParameterValueByKey(parameters, Parameter.KEY_LAST_CALL) );
            long currentTime = System.currentTimeMillis();
            if (isTimeExpired(currentTime, hour, min, lastCall)) {
                log.debug("time {} is expired, lastCall: {} milli sec, currentTime: {} milli sec", timeStr, lastCall, currentTime);
                return true;
            }
        }
        return false;
    }

    public static boolean isTimeExpired(long currentTime, long hourTime, long minTime, long lastCall) {
        long lcHour = getHours(lastCall);
        long lcMin = getMinutes(lastCall);
        long nowHour = getHours(currentTime);
        long nowMin = getMinutes(currentTime);
        boolean laterThanDefinedTime = (nowHour * 60 + nowMin ) >= (hourTime * 60 + minTime );
        boolean lastCallEarlierThanDefinedTime = (lcHour * 60 + lcMin ) < (hourTime * 60 + minTime );
        return laterThanDefinedTime && lastCallEarlierThanDefinedTime;
    }

    private static String formattingAndCleaningOfFixedDelay(String parameterValue) {
        String value = parameterValue;
        value = value.toLowerCase();
        value = value.replaceAll(",", "");
        value = value.replaceAll(";", "");
        value = value.replaceAll("_", "");
        value = value.replaceAll("-", "");

        value = value.replaceAll("weeks", "w");
        value = value.replaceAll("week", "w");

        value = value.replaceAll("days", "d");
        value = value.replaceAll("day", "d");

        value = value.replaceAll("hours", "h");
        value = value.replaceAll("hour", "h");

        value = value.replaceAll("minutes", "m");
        value = value.replaceAll("minute", "m");
        value = value.replaceAll("mins", "m");
        value = value.replaceAll("min", "m");

        value = value.replaceAll("seconds", "s");
        value = value.replaceAll("second", "s");
        value = value.replaceAll("secs", "s");
        value = value.replaceAll("sec", "s");


        value = value.replaceAll("w", "w ");
        value = value.replaceAll("h", "h ");
        value = value.replaceAll("m", "m ");
        value = value.replaceAll("d", "d ");
        value = value.replaceAll("s", "s ");
        while(value.contains("  ")) value = value.replaceAll("  ", " ");

        return value.trim();
    }

    private static long getTimeSection(String timeSection) {
        long time = 0;
        Character c = timeSection.charAt(timeSection.length()-1);
        long l = Integer.valueOf(timeSection.substring(0, timeSection.length()-1));

        switch (c) {
            case 's':
                time += inMilliFromSecs(l);
                break;
            case 'm':
                time += inMilliFromMins(l);
                break;
            case 'h':
                time += inMilliFromHours(l);
                break;
            case 'd':
                time += inMilliFromDays(l);
                break;
            case 'w':
                time += inMilliFromWeeks(l);
                break;
            default:
                throw new RuntimeException("Not implemented: "+c);
        }
        return time;
    }

    private static long inMilliFromSecs(long sec) {
        return 1000*sec;
    }

    private static long inMilliFromMins(long min) {
        return inMilliFromSecs(min) *60;
    }

    private static long inMilliFromHours(long h) {
        return inMilliFromMins(h)*60;
    }

    private static long inMilliFromDays(long d) {
        return inMilliFromHours(d)*24;
    }

    private static long inMilliFromWeeks(long w) {
        return inMilliFromDays(w)*7;
    }

    public static long getHours(long timestamp) {
        ZoneOffset o = OffsetDateTime.now().getOffset();
        int offset = o.getTotalSeconds()/3600;
        return (timestamp / (60 * 60 * 1000)) % 24 + offset;
    }

    public static long getMinutes(long timestamp) {
        return (timestamp / (60 *1000) ) % 60;
    }
}
