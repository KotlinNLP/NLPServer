#!/bin/bash

tokenizer_models_directory="models/tokenizers"
language_detector_model="models/language_detector.serialized"
cjk_tokenizer_model="models/cjk_tokenizer.serialized"
frequency_dictionary="models/frequency_dictionary.serialized"
morphology_dictionary="models/morphology_dictionary"
neural_parser="models/neural_parser"
frame_extractor="models/frame_extractor"
frame_extractor_embeddings="models/frame_extractor_embeddings"
han_classifier="models/han_classifier"
han_classifier_embeddings="models/han_classifier_embeddings"
locations_dictionary="models/locations_dictionary.serialized"

parameters="-p $1"
    
if [ -f $tokenizer_models_directory/* ]; then
    echo "Loaded tokenizers:"
    ls -1 $tokenizer_models_directory
    parameters=$parameters" -t $tokenizer_models_directory"
fi

if [ -f $cjk_tokenizer_model ]; then
    echo "Loaded cjk tokenizer:"
    echo $cjk_tokenizer_model
    parameters=$parameters" -c $cjk_tokenizer_model"
fi

if [ -f $language_detector_model ]; then
    echo "Loaded language detector:"
    echo $language_detector_model
    parameters=$parameters" -l $language_detector_model"
fi

if [ -f $frequency_dictionary ]; then
    echo "Loaded frequency dictionary:"
    echo $frequency_dictionary
    parameters=$parameters" -f $frequency_dictionary"
fi

if [ -f $morphology_dictionary/* ]; then
    echo "Loaded morphology dictionaries:"
    ls -1 $morphology_dictionary
    parameters=$parameters" -m $morphology_dictionary"
fi

if [ -f $neural_parser/* ]; then
    echo "Loaded neural parsers:"
    ls -1 $neural_parser
    parameters=$parameters" -n $neural_parser"
fi

if [ -f $frame_extractor/* ]; then
    echo "Loaded frame extractors:"
    ls -1 $frame_extractor
    parameters=$parameters" -x $frame_extractor"
fi

if [ -f $frame_extractor_embeddings/* ]; then
    echo "Loaded frame extractors embeddings:"
    ls -1 $frame_extractor_embeddings
    parameters=$parameters" -e $frame_extractor_embeddings"
fi

if [ -f $han_classifier/* ]; then
    echo "Loaded han classifiers:"
    ls -1 $han_classifier
    parameters=$parameters" -s $han_classifier"
fi

if [ -f $han_classifier_embeddings/* ]; then
    echo "Loaded han classifiers embeddings:"
    ls -1 $han_classifier_embeddings
    parameters=$parameters" -g $han_classifier_embeddings"
fi

if [ -f $locations_dictionary ]; then
    echo "Loaded locations dictionary:"
    echo $locations_dictionary
    parameters=$parameters" -d $locations_dictionary"
fi

echo "#!/bin/bash" > docker-entrypoint.sh
echo "java -jar nlpserver.jar $parameters" >> docker-entrypoint.sh
chmod +x ./docker-entrypoint.sh
