#!/bin/bash

set -e

tokenizer_models_directory="models/tokenizers"
language_detector_model="models/language_detector.serialized"
cjk_tokenizer_model="models/tokenizer_cjk.serialized"
frequency_dictionary="models/frequency_dictionary.serialized"
morphology_dictionaries="models/morphology_dictionaries"
neural_parsers="models/neural_parsers"
frame_extractors="models/frame_extractors"
frame_extractor_embeddings="models/frame_extractor_embeddings"
han_classifiers="models/han_classifiers"
han_classifier_embeddings="models/han_classifier_embeddings"
locations_dictionary="models/locations_dictionary.serialized"

parameters="-p $1"

if [[ -d "$tokenizer_models_directory" ]] && [[ -n $(ls -A "$tokenizer_models_directory") ]]; then
    echo "Loaded tokenizers:"
    ls -1 ${tokenizer_models_directory}
    parameters="$parameters -t $tokenizer_models_directory"
fi

if [[ -f "$cjk_tokenizer_model" ]]; then
    echo "Loaded cjk tokenizer:"
    echo ${cjk_tokenizer_model}
    parameters="$parameters -c $cjk_tokenizer_model"
fi

if [[ -f "$language_detector_model" ]]; then
    echo "Loaded language detector:"
    echo ${language_detector_model}
    parameters="$parameters -l $language_detector_model"
fi

if [[ -f "$frequency_dictionary" ]]; then
    echo "Loaded frequency dictionary:"
    echo ${frequency_dictionary}
    parameters="$parameters -f $frequency_dictionary"
fi

if [[ -d "$morphology_dictionaries" ]] && [[ -n $(ls -A "$morphology_dictionaries") ]]; then
    echo "Loaded morphology dictionaries:"
    ls -1 ${morphology_dictionaries}
    parameters="$parameters -m $morphology_dictionaries"
fi

if [[ -d "$neural_parsers" ]] && [[ -n $(ls -A "$neural_parsers") ]]; then
    echo "Loaded neural parsers:"
    ls -1 ${neural_parsers}
    parameters="$parameters -n $neural_parsers"
fi

if [[ -d "$frame_extractors" ]] && [[ -n $(ls -A "$frame_extractors") ]]; then
    echo "Loaded frame extractors:"
    ls -1 ${frame_extractors}
    parameters="$parameters -x $frame_extractors"
fi

if [[ -d "$frame_extractor_embeddings" ]] && [[ -n $(ls -A "$frame_extractor_embeddings") ]]; then
    echo "Loaded frame extractors embeddings:"
    ls -1 ${frame_extractor_embeddings}
    parameters="$parameters -e $frame_extractor_embeddings"
fi

if [[ -d "$han_classifiers" ]] && [[ -n $(ls -A "$han_classifiers") ]]; then
    echo "Loaded han classifiers:"
    ls -1 ${han_classifiers}
    parameters="$parameters -s $han_classifiers"
fi

if [[ -d "$han_classifier_embeddings" ]] && [[ -n $(ls -A "$han_classifier_embeddings") ]]; then
    echo "Loaded han classifiers embeddings:"
    ls -1 ${han_classifier_embeddings}
    parameters="$parameters -g $han_classifier_embeddings"
fi

if [[ -f "$locations_dictionary" ]]; then
    echo "Loaded locations dictionary:"
    echo ${locations_dictionary}
    parameters="$parameters -d $locations_dictionary"
fi

echo "#!/bin/bash" > docker-entrypoint.sh
echo "java -Xmx16g -jar nlpserver.jar $parameters" >> docker-entrypoint.sh
chmod +x ./docker-entrypoint.sh
