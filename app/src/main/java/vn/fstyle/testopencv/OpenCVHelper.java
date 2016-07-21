package vn.fstyle.testopencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * OpenCVHelper.
 *
 * @author DaoLQ
 */
public class OpenCVHelper {
    private static final String TAG = OpenCVHelper.class.getSimpleName();

    // The threshold value for detect blur photo.
    private static final int BLUR_THRESHOLD = 110;
    // The threshold value for detect similar photo.
    private static final int SIMILAR_THRESHOLD = 200;

    // The threshold value for detect dark photo.
    private static final int TOLERANCE = 120;
    private static final float DARK_PERCENT = 0.8F;

    // The threshold value for detect memo photo.
    private static final float MEMO_THRESHOLD_DARK_BOARD = 0.55F;
    private static final float MEMO_THRESHOLD_WHITE_BOARD = 0.68F;


    /**
     * This is used to define the type of photo.
     */
    enum TypeBoard {
        DARK_BOARD, WHITE_BOARD
    }

    /**
     * This is used to define the type to compare histogram of photo.
     */
    enum TypeCompare {

        CORRELATION(Imgproc.CV_COMP_CORREL), CHI_SQUARED(Imgproc.CV_COMP_CORREL),
        INTERSECTION(Imgproc.CV_COMP_INTERSECT), HELLINGER(Imgproc.CV_COMP_BHATTACHARYYA);

        private final int value;

        TypeCompare(int val) {
            this.value = val;
        }

        int getValue() {
            return this.value;
        }
    }

    private OpenCVHelper() {
    }

    static boolean isBlurPhoto(@NonNull String fileName) {

        if (isDarkPhoto(fileName)) {
            return false;
        }

        try {
            Bitmap bitmap = getBitmapToDetect(fileName);
            bitmap = ImageUtil.resizeImage(bitmap, 500);
            bitmap = ImageUtil.cropByPercentage(bitmap, 0.5F);
            Mat src = new Mat();
            Utils.bitmapToMat(bitmap, src);
            bitmap.recycle();

            // Convert image to gray
            Mat gray = new Mat();
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
            src.release();

            Mat lap = new Mat();
            Imgproc.Laplacian(gray, lap, CvType.CV_8U);
            gray.release();

            Bitmap image = Bitmap.createBitmap(lap.cols(), lap.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(lap, image);
            lap.release();

            int[] pixels = new int[image.getHeight() * image.getWidth()];
            image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            image.recycle();

            int maxLap = Integer.MIN_VALUE;
            for (int value : pixels) {
                if ((value & 0xFF) > maxLap) {
                    // Read the pixel value - the 0xFF is needed to cast
                    // from an unsigned byte to an int.
                    maxLap = value & 0xFF;
                }
            }

            return maxLap < BLUR_THRESHOLD;
        } catch (Exception e) {
            Log.e(TAG, "Detect blur error: ", e);
            return false;
        }
    }

    /**
     * This method is used to check an image is dark image or not.
     *
     * @param fileName is path of image file.
     * @return return true if input image is dark else return false.
     */
    static boolean isDarkPhoto(@NonNull String fileName) {

        try {
            // Load image to bitmap
            Bitmap image = getBitmapToDetect(fileName);
            if (image == null) {
                return false;
            }
            image = ImageUtil.resizeImage(image, 50);

            // Get pixels from bitmap to array of int
            int[] pixels = new int[image.getHeight() * image.getWidth()];
            image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            image.recycle();

            int count = 0;
            for (int value : pixels) {

                int redValue = Color.red(value);
                int blueValue = Color.blue(value);
                int greenValue = Color.green(value);
                double brightness = Math.round(0.299 * redValue
                        + 0.5876 * greenValue
                        + 0.114 * blueValue);

                if (brightness < TOLERANCE) {
                    count++;
                }
            }

            // Calculate the dark percent of image
            double imageDarkPercent = (double) count / (double) (image.getWidth() * image.getHeight());

            return imageDarkPercent > DARK_PERCENT;
        } catch (Exception e) {
            Log.e(TAG, "Detect dark photo error.", e);
            return false;
        }
    }

    /**
     * This method is used to detect image is screenshot.
     *
     * @param context  is current context.
     * @param fileName is image file name.
     * @return return true if image input is screenshot image else return false.
     */
    static boolean isScreenshot(@NonNull Context context, @NonNull String fileName) {

        try {

            if (!fileName.toLowerCase(Locale.getDefault()).endsWith(".png")) {
                return false;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, options);
            int imageHeight = options.outHeight;
            int imageWidth = options.outWidth;

            ScreenUtil.ScreenSize screenSize = ScreenUtil.getScreenSize(context);
            Log.d(TAG, "Image height: " + imageHeight);
            Log.d(TAG, "Screen height: " + screenSize.getHeight());

            return (imageHeight == screenSize.getHeight() && imageWidth == screenSize.getWidth());
        } catch (Exception e) {
            Log.e(TAG, "Detect screen shot error: ", e);
            return false;
        }
    }

    static boolean isSimilarPhoto(@NonNull String sourceFile,
                                  @NonNull String comparingFile) {
        if (isDarkPhoto(sourceFile)) {
            return false;
        }

        if (isDarkPhoto(comparingFile)) {
            return false;
        }
        try {
            Bitmap bitmap1 = getBitmapToDetect(sourceFile);
            bitmap1 = ImageUtil.resizeImage(bitmap1, 500);
            Mat source = new Mat();
            Utils.bitmapToMat(bitmap1, source);
            bitmap1.recycle();

            Bitmap bitmap2 = getBitmapToDetect(comparingFile);
            bitmap2 = ImageUtil.resizeImage(bitmap2, 500);
            Mat comparing = new Mat();
            Utils.bitmapToMat(bitmap2, comparing);
            bitmap2.recycle();

            MatOfKeyPoint keyPoints1 = new MatOfKeyPoint();
            MatOfKeyPoint keyPoints2 = new MatOfKeyPoint();
            Mat descriptors1 = new Mat();
            Mat descriptors2 = new Mat();

            // Definition of ORB key point detector and descriptor extractors
            FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
            DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

            // Detect key points
            detector.detect(source, keyPoints1);
            detector.detect(comparing, keyPoints2);

            // Extract descriptors
            extractor.compute(source, keyPoints1, descriptors1);
            extractor.compute(comparing, keyPoints2, descriptors2);

            // Definition of descriptor matcher
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            // Match points of two images
            MatOfDMatch matches = new MatOfDMatch();
            matcher.match(descriptors1, descriptors2, matches);

            // Sort
            List<DMatch> matchesList = matches.toList();
            Collections.sort(matchesList, new DMatchComparator());

            // Release image.
            source.release();
            comparing.release();
            descriptors1.release();
            descriptors2.release();

            final int size = matchesList.size();
            if (size == 0) {
                return false;
            }
            Log.d(TAG, "matchesList size: " + size);

            int sumDistance = 0;
            for (int i = 0; i < size; i++) {
                sumDistance += matchesList.get(i).distance;
                if (i == 10) {
                    break;
                }
            }
            Log.d(TAG, "sumDistance: " + sumDistance);

            return sumDistance < SIMILAR_THRESHOLD;
        } catch (Exception e) {
            Log.e(TAG, "Detect similar error.", e);
            return false;
        }
    }

    /**
     * This method is used to detect memo photo.
     *
     * @param photoUrl      is url of photo need to detect.
     * @param typeCompare   is type for comparing.
     * @param typeBoard     is type of board.
     * @param baseCalcHists is list of base calcHist.
     * @return return true if photo is memo photo else return false.
     */
    static boolean isMemoPhoto(@NonNull String photoUrl, @NonNull TypeCompare typeCompare,
                               @NonNull TypeBoard typeBoard, @NonNull Mat... baseCalcHists) {

        try {
            Mat calcHist = getCalcHist(photoUrl);
            double sum = 0;
            for (Mat mat : baseCalcHists) {
                sum += Imgproc.compareHist(mat, calcHist, typeCompare.getValue());
            }
            calcHist.release();

            double avg = sum / (double) baseCalcHists.length;

            float threshold;
            if (typeBoard == TypeBoard.DARK_BOARD) {
                threshold = MEMO_THRESHOLD_DARK_BOARD;
            } else {
                threshold = MEMO_THRESHOLD_WHITE_BOARD;
            }

            if ((typeCompare == TypeCompare.CORRELATION)
                    || (typeCompare == TypeCompare.INTERSECTION)) {
                if (avg >= threshold) {
                    return true;
                }
            } else if ((typeCompare == TypeCompare.CHI_SQUARED)
                    || (typeCompare == TypeCompare.HELLINGER)) {
                if (avg <= threshold) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Detect memo photo error: ", e);
            return false;
        }
    }

    private static Mat getCalcHist(@NonNull String photoUrl) {
        Mat mat = Imgcodecs.imread(photoUrl);
        Mat result = getCalcHist(mat);
        mat.release();

        return result;
    }

    static Mat getCalcHist(@NonNull Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        return getCalcHist(mat);
    }

    private static Mat getCalcHist(@NonNull Mat image) {
        try {
            List<Mat> list = new ArrayList<>();
            list.add(image);

            MatOfInt histSize = new MatOfInt(8, 8, 8);
            MatOfFloat ranges = new MatOfFloat(0F, 256F, 0F, 256F, 0F, 256F);
            MatOfInt channels = new MatOfInt(0, 1, 2);

            Mat hist = new Mat();
            Mat mask = new Mat();
            Imgproc.calcHist(list, channels, new Mat(), hist, histSize, ranges);
            Core.normalize(hist, hist, 1, 0, Core.NORM_L2, -1, mask);

            histSize.release();
            ranges.release();
            channels.release();
            mask.release();

            return hist;
        } catch (Exception e) {
            Log.e(TAG, "get photo's histogram error: ", e);
            return null;
        }
    }

    /**
     * This method is used to detect decorated image else return false.
     *
     * @param photoUrl is path of image file.
     * @return return true if image is a decorated photo.
     */
    static boolean isDecoratedPhoto(@NonNull String photoUrl) {
        final String[] targetDecoratedApps = {
                "BeautyPlus", "Instagram", "aillis"
        };

        for (String appName : targetDecoratedApps) {
            if (photoUrl.contains(appName)) {
                return true;
            }
        }

        return false;
    }

    static boolean isVideo1Second(@NonNull Context context, @NonNull String fileName) {

        try {
            File videoFile = new File(fileName);
            if (!videoFile.exists()) {
                return false;
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, Uri.fromFile(videoFile));
            int duration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            retriever.release();

            duration = duration / 1000;

            return duration == 1;
        } catch (Exception e) {
            Log.e(TAG, "Detect video 1 second error: ", e);
            return false;
        }
    }

    private static Bitmap getBitmapToDetect(@NonNull String fileName) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // Originally, we need to set 8 for memory,
        // but small image cannot create match points.
        options.inSampleSize = 1;
        // If crashed image, BitmapFactory.decodeFile returns null
        return BitmapFactory.decodeFile(fileName, options);
    }

    /**
     * This class is used to compare the DMatch.
     */
    private static class DMatchComparator implements Comparator<DMatch> {
        public int compare(DMatch left, DMatch right) {
            return Float.compare(left.distance, right.distance);
        }
    }

}
