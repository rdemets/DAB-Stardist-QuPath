/**
 * This script provides a general template for cell detection using StarDist in QuPath.
 * 
 * This example assumes you have brightfield image, but want to apply preprocessing 
 * to separate the stains before running a model trained for fluorescence images.
 * One reason to do this is to handle some arbitrary IHC staining if you don't have 
 * a model trained specifically for your kind of images.
 * 
 * If you use this in published work, please remember to cite *both*:
 *  - the original StarDist paper (https://doi.org/10.48550/arXiv.1806.03535)
 *  - the original QuPath paper (https://doi.org/10.1038/s41598-017-17204-5)
 *  
 * There are lots of options to customize the detection - this script shows some 
 * of the main ones. Check out other scripts and the QuPath docs for more info.
 */
clearDetections();
selectAnnotations()

import qupath.ext.stardist.StarDist2D
import qupath.lib.scripting.QP

// IMPORTANT! Replace this with the path to your StarDist model
// that takes a single channel as input (e.g. dsb2018_heavy_augment.pb)
// You can find some at https://github.com/qupath/models
// (Check credit & reuse info before downloading)
def modelPath = "C:/Users/CFIM/QuPath/v0.5/extensions/dsb2018_heavy_augment.pb"

// Get current image - assumed to have color deconvolution stains set
var imageData = getCurrentImageData()
var stains = imageData.getColorDeconvolutionStains()

// Customize how the StarDist detection should be applied
// Here some reasonable default options are specified
def stardist = StarDist2D
    .builder(modelPath)
    .preprocess( // Extra preprocessing steps, applied sequentially
        ImageOps.Channels.deconvolve(stains), // Color deconvolution
        ImageOps.Channels.extract(1),         // Extract the first stain (indexing starts at 0)
        ImageOps.Filters.median(2)           // Apply a small median filter (optional!)
    )
    .normalizePercentiles(1, 99) // Percentile normalization
    .threshold(0.7)              // Probability (detection) threshold
    .pixelSize(0.2)              // Resolution for detection
    .measureShape()              // Add shape measurements
    .measureIntensity()          // Add cell measurements (in all compartments)
    .build()
	
// Define which objects will be used as the 'parents' for detection
// Use QP.getAnnotationObjects() if you want to use all annotations, rather than selected objects
def pathObjects = QP.getSelectedObjects()

// Run detection for the selected objects
if (pathObjects.isEmpty()) {
    QP.getLogger().error("No parent objects are selected!")
    return
}
stardist.detectObjects(imageData, pathObjects)


// Filter Cell by size
def toDeleteLarge = getDetectionObjects().findAll {measurement(it, 'Area µm^2') > 150}
def toDeleteSmall = getDetectionObjects().findAll {measurement(it, 'Area µm^2') < 20}

//Filter Cell by DAB intensity
def toDeleteInt = getDetectionObjects().findAll {measurement(it, 'DAB: Mean') < 0.57}
removeObjects(toDeleteLarge, true)
removeObjects(toDeleteSmall, true)
removeObjects(toDeleteInt, true)


stardist.close() // This can help clean up & regain memory
println('Done!')