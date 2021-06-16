import json
import pickle
import random
import nltk
import numpy

from nltk.stem import LancasterStemmer
from tensorflow.python.keras.layers import Dense
from tensorflow.python.keras.models import Sequential
from tensorflow.python.keras.models import model_from_yaml

# download nltk data
data = nltk.download('punkt') # nltk data, punkt contain enlish token and others language

# stem word using Lancaster algorithm
stemmer = LancasterStemmer() # LancasterStemmer class

# open intents.json file and store in data
with open("intents.json") as file:
    data = json.load(file)

# if it already had chatbot.pickle then load it
try:
    with open("chatbot.pickle", "rb") as file:
        words, labels, training, output = pickle.load(file)

# if not
except:
    words = []  # words list
    labels = [] # labels list
    docs_x = [] # word
    docs_y = [] # tag

    # data(intents) is a list of dictionaries
    for intent in data["intents"]:
        for pattern in intent["patterns"]:
            wrds = nltk.word_tokenize(pattern) # tokenize each word in patterns
            words.extend(wrds)  # extend word into words
            docs_x.append(wrds) # append word to docs_x, x -> word
            docs_y.append(intent["tag"]) # append tag value to docs_y, y -> tag

        # add tags value to labels(no same value)
        if intent["tag"] not in labels:
            labels.append(intent["tag"])

    # stem word(lower case and no "?")
    words = [stemmer.stem(w.lower()) for w in words if w != "?"]
    # sort word(no the same)
    words = sorted(list(set(words)))

    # sort label (tag)
    labels = sorted(labels)

    training = []
    output = []

    output_empty = [0 for _ in range(len(labels))] # '_' represent the index of each element in a list

    # enumerate(iterable) retrun index, value
    for x, doc in enumerate(docs_x):
        bag = []

        wrds = [stemmer.stem(w.lower()) for w in doc]

        for w in words:
            if w in wrds:
                bag.append(1)
            else:
                bag.append(0)

        output_row = output_empty[:]
        output_row[labels.index(docs_y[x])] = 1

        training.append(bag)
        output.append(output_row)

    training = numpy.array(training)
    output = numpy.array(output)

    with open("chatbot.pickle", "wb") as file:
        pickle.dump((words, labels, training, output), file)

try:
    yaml_file = open('chatbotmodel.yaml', 'r')
    loaded_model_yaml = yaml_file.read()
    yaml_file.close()
    myChatModel = model_from_yaml(loaded_model_yaml)
    myChatModel.load_weights("chatbotmodel.h5")
    print("Loaded model from disk")

except:
    # Make our neural network
    myChatModel = Sequential()
    myChatModel.add(Dense(8, input_shape=[len(words)], activation='relu'))
    myChatModel.add(Dense(len(labels), activation='softmax'))

    # optimize the model
    myChatModel.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])

    # train the model
    myChatModel.fit(training, output, epochs=1000, batch_size=8)

    # serialize model to yaml and save it to disk
    model_yaml = myChatModel.to_yaml()
    with open("chatbotmodel.yaml", "w") as y_file:
        y_file.write(model_yaml)

    # serialize weights to HDF5
    myChatModel.save_weights("chatbotmodel.h5")
    print("Saved model from disk")


def bag_of_words(s, words):
    bag = [0 for _ in range(len(words))]

    s_words = nltk.word_tokenize(s)
    s_words = [stemmer.stem(word.lower()) for word in s_words]

    for se in s_words:
        for i, w in enumerate(words):
            if w == se:
                bag[i] = 1

    return numpy.array(bag)


def chatWithBot(inputText):
    currentText = bag_of_words(inputText, words)
    currentTextArray = [currentText]
    numpyCurrentText = numpy.array(currentTextArray)

    if numpy.all((numpyCurrentText == 0)):
        return "I didn't get that, try again"

    result = myChatModel.predict(numpyCurrentText[0:1])
    result_index = numpy.argmax(result)
    tag = labels[result_index]

    if result[0][result_index] > 0.7:
        for tg in data["intents"]:
            if tg['tag'] == tag:
                responses = tg['responses']

        return random.choice(responses)

    else:
        return "I didn't get that, try again"


def chat():
    print("Start talking with the chatbot (try quit to stop)")

    while True:
        inp = input("You: ")
        if inp.lower() == "quit":
            break

        print(chatWithBot(inp))

# chat()