package net.igorkromin.jphotoframe.ui.widgets;

import net.igorkromin.jphotoframe.ui.ModelData;
import net.igorkromin.jphotoframe.weather.Forecast;
import net.igorkromin.jphotoframe.weather.Weather;
import net.igorkromin.jphotoframe.weather.WeatherConditionCodes;
import org.json.JSONObject;

import java.awt.*;

import java.util.ArrayList;
import java.util.List;

import static net.igorkromin.jphotoframe.ui.widgets.Factory.*;

public class WeatherForecast extends Transformable {

    private static final String DATA_SRC_TEMPERATURE = "$temperature";

    private static final String KEY_ITEMS = "items";
    private static final String KEY_ITEM_GAP = "gap";
    private static final String KEY_GAP_POSITION = "gapPosition";
    private static final String KEY_ORIENTATION = "orientation";

    private static final String GAP_POS_LEADING = "leading";
    private static final String GAP_POS_TRAILING = "trailing";

    private static final String ORIENTATION_HORZ = "horizontal";
    private static final String ORIENTATION_VERT = "vertical";

    private static final int DEFAULT_GAP_SIZE = 150;
    private static final int DEFAULT_GAP_SCALAR = 0;
    private static final int DEFAULT_ORIENTATION_SX = 1;
    private static final int DEFAULT_ORIENTATION_SY = 0;

    private ModelData data;

    private Rectangle bounds = new Rectangle(0, 0);
    private JSONObject textConfig;
    private List<Text> textWidgets = new ArrayList<>();
    private List<Text> drawList = new ArrayList<>();

    private int itemGap = DEFAULT_GAP_SIZE;
    private int itemBoundScalar = DEFAULT_GAP_SCALAR;
    private int orientationSX = DEFAULT_ORIENTATION_SX;
    private int orientationSY = DEFAULT_ORIENTATION_SY;

    public WeatherForecast(JSONObject json, ModelData data, Rectangle drawAreaBounds) {
        super(json.getJSONObject(KEY_TRANSFORM), drawAreaBounds);

        this.data = data;

        // - text
        JSONObject text =  (json.has(KEY_TEXT)) ? json.getJSONObject(KEY_TEXT) : new JSONObject();
        textConfig = new JSONObject();
        textConfig.put(KEY_TEXT, text);

        // - items
        if (json.has(KEY_ITEMS)) {
            JSONObject items = json.getJSONObject(KEY_ITEMS);

            // - gap
            if (items.has(KEY_ITEM_GAP)) {
                itemGap = items.getInt(KEY_ITEM_GAP);
            }

            // - gapPosition
            if (items.has(KEY_GAP_POSITION)) {
                String pos = items.getString(KEY_GAP_POSITION);

                if (GAP_POS_LEADING.equals(pos)) {
                    itemBoundScalar = 0;
                }
                else if (GAP_POS_TRAILING.equals(pos)) {
                    itemBoundScalar = 1;
                }
            }

            // - orientation
            if (items.has(KEY_ORIENTATION)) {
                String orientation = items.getString(KEY_ORIENTATION);

                if (ORIENTATION_HORZ.equals(orientation)) {
                    orientationSX = 1;
                    orientationSY = 0;
                }
                else if (ORIENTATION_VERT.equals(orientation)) {
                    orientationSX = 0;
                    orientationSY = 1;
                }
            }
        }
    }

    @Override
    public Rectangle syncModelToBounds(Graphics2D graphics) {

        Weather weather = data.getWeather();
        if (weather != null && weather.getForecast() != null) {
            List<Forecast> forecastList = weather.getForecast();
            int forecasts = forecastList.size();

            int width = 0;
            int height = 0;

            adjustWidgetList(forecasts);
            drawList.clear();

            for (int i = 0; i < forecasts; i++) {
                Forecast forecast = forecastList.get(i);
                Text text = textWidgets.get(i);

                WeatherConditionCodes code = WeatherConditionCodes.fromInt(forecast.getCode());

                text.overwriteDataSource(code.getInfoText());
                Rectangle bounds = text.syncModelToBounds(graphics);

                if (bounds != null) {
                    width += (bounds.width * ((i < forecasts - 1) ? itemBoundScalar : 1)) + ((i < forecasts - 1) ? itemGap : 0);
                    height = (bounds.height > height) ? bounds.height : height;

                    drawList.add(text);
                }
            }

            bounds.setBounds(0,0, width, height);
            return bounds;
        }

        return null;
    }

    /**
     * Adjusts the text widget list to have one widget per forecast day.
     * @param newSize
     */
    private void adjustWidgetList(int newSize) {
        int size = textWidgets.size();

        if (newSize == size) {
            return;
        }

        // grow the widget list
        if (size < newSize) {
            while (size < newSize) {
                textWidgets.add(new Text(textConfig, data, getDrawAreaBounds()));
                size++;
            }
        }
        // shrink the widget list
        else {
            while (size > newSize) {
                textWidgets.remove(size - 1);
                size--;
            }
        }
    }

    @Override
    public void drawTransformed(Graphics2D graphics) {
        int tx = 0;

        for (Text text : drawList) {
            // copy the Graphics2D object to avoid incompatible state changes
            Graphics2D graphics2 = (Graphics2D) graphics.create();

            graphics2.translate(tx, 0);
            text.drawTransformed(graphics2);

            tx += (text.getTextBounds().width * itemBoundScalar) + itemGap;
        }
    }

}
