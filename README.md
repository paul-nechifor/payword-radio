PayWord implementation
======================

An implementation of the [PayWord micropayment scheme][1] as an audio streaming
payment method. PayWord is an efficient and secure way to do many small
payments, in this case paying for seconds of received audio files.

I really liked this project and found it very practical. Contact me if you are
interested in doing something similar in a real setting.

This was one of my homeworks for the [Information Security][2] course.

This project is written in Java and can be run as three different roles:

* a **broker** who manages the money and is trusted;
* a **vendor** which sends audio data and expects to be payed;
* a GUI **client** which can connect to a vendor to receive and play audio data
and pays for it.

Running it
----------

These two files are missing from here because they are copyrighted:

* `files/downstream.wav`
* `files/dreamland.wav`

They should be replaced by any WAV files.

[1]: http://people.csail.mit.edu/rivest/RivestShamir-mpay.pdf
[2]: http://www.infoiasi.ro/bin/Programs/CS3102_11
