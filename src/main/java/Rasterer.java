import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {

    private static final double Lpp_Graph0 = (MapServer.ROOT_LRLON - MapServer.ROOT_ULLON) / MapServer.TILE_SIZE;
    public Rasterer() {
        //YOU CODE HERE
    }
    private static int scaleLevel(double lonGap, double w){
        double lonPerPix = lonGap/w;
        int res = 0;
        double lpp_GraphNow = Lpp_Graph0;
        while(lpp_GraphNow > lonPerPix && res < 7){
            lpp_GraphNow /= 2;
            res += 1;
        }
        return res;
    }
    private static int scale = 7;

    private static Map<String, Double> calcTopLeft(double ullon, double ullat){
        double lonGap = ullon - MapServer.ROOT_ULLON;
        double latGap = MapServer.ROOT_ULLAT - ullat;
        double originLatGap = MapServer.ROOT_ULLAT - MapServer.ROOT_LRLAT;
        /* double to int to double is key here*/
        double upPos =  (int)(latGap*Math.pow(2, scale)/originLatGap); //cant use Sl
        double originLonGap = MapServer.ROOT_LRLON - MapServer.ROOT_ULLON;
        double leftPos =  (int)(lonGap*Math.pow(2, scale)/originLonGap);
        double rasterULLat = MapServer.ROOT_ULLAT - originLatGap*(upPos/Math.pow(2, scale));
        double rasterULLon = MapServer.ROOT_ULLON + originLonGap*(leftPos/Math.pow(2, scale));
        Map res = new HashMap<>();
        res.put("YUp", upPos);
        res.put("XLeft", leftPos);
        res.put("ULLat", Math.min(MapServer.ROOT_ULLAT, rasterULLat));
        res.put("ULLon", Math.max(MapServer.ROOT_ULLON, rasterULLon));
        return res;
    }

    private static int top, left, bottom, right;
    private static Map<String, Double> calcBottomRight(double lrlon, double lrlat){
        double lonGap = lrlon - MapServer.ROOT_ULLON;
        double latGap = MapServer.ROOT_ULLAT - lrlat;
        double originLatGap = MapServer.ROOT_ULLAT - MapServer.ROOT_LRLAT;
        double downPos =  (int)(latGap*Math.pow(2, scale)/originLatGap);
        double originLonGap = MapServer.ROOT_LRLON - MapServer.ROOT_ULLON;
        double rightPos =  (int)(lonGap*Math.pow(2, scale)/originLonGap);
        double rasterLRLat = MapServer.ROOT_ULLAT - originLatGap*((downPos+1)/Math.pow(2, scale));
        double rasterLRLon = MapServer.ROOT_ULLON + originLonGap*((rightPos+1)/Math.pow(2, scale));
        Map res = new HashMap<>();
        res.put("YDown", downPos);
        res.put("XRight", rightPos);
        res.put("LRLat", Math.max(MapServer.ROOT_LRLAT, rasterLRLat));
        res.put("LRLon", Math.min(MapServer.ROOT_LRLON, rasterLRLon));
        return res;
    }

    private static String[][] generateGrid(){
        int width = Math.max(0, right - left + 1);
        if(width == 0){
            return new String[0][0];
        }
        int height = Math.max(0, bottom - top + 1);
        String[][] filenames = new String[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int x = left + j;
                int y = top + i;
                filenames[i][j] = "d" + scale + "_x" + x + "_y" + y + ".png";
            }
        }
        return filenames;
    }
    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        System.out.println(params);
        Map<String, Object> results = new HashMap<>();
        double lonGap = params.get("lrlon") - params.get("ullon");
        scale = scaleLevel(lonGap, params.get("w"));
        int maxPicNum = (int) (Math.pow(2, scale)) - 1;

        Map<String, Double>topLeftRes = calcTopLeft(params.get("ullon"), params.get("ullat"));
        double raster_ul_lon = topLeftRes.get("ULLon"); //Math.max()
        double raster_ul_lat = topLeftRes.get("ULLat");
        top = (int)Math.max(0, topLeftRes.get("YUp"));
        left = (int)Math.max(0, topLeftRes.get("XLeft"));

        Map<String, Double>bottomRightRes = calcBottomRight(params.get("lrlon"), params.get("lrlat"));
        double raster_lr_lon = bottomRightRes.get("LRLon");
        double raster_lr_lat = bottomRightRes.get("LRLat");
        bottom = (int)Math.min(maxPicNum, bottomRightRes.get("YDown"));
        right = (int)Math.min(maxPicNum, bottomRightRes.get("XRight"));

        String[][] render_grid = generateGrid();

        boolean query_success = (render_grid.length > 0);

        results.put("render_grid", render_grid);
        results.put("raster_ul_lon", raster_ul_lon);
        results.put("raster_ul_lat", raster_ul_lat);
        results.put("raster_lr_lon", raster_lr_lon);
        results.put("raster_lr_lat", raster_lr_lat);
        results.put("depth", scale);
        results.put("query_success", query_success);

        return results;
    }

    public static void main(String[] args) {
        //System.out.println(scaleLevel(0.241632 - 0.24053, 892.0));
        calcTopLeft(-122.241632, 37.87655);
        calcBottomRight(-122.24053, 37.87548);
    }
}
