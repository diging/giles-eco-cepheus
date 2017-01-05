# Cepheus 
## giles-eco-cepheus

<a href='http://diging-dev.asu.edu/jenkins/job/GECO_test_cepheus_on_push'><img src='http://diging-dev.asu.edu/jenkins/buildStatus/icon?job=GECO_test_cepheus_on_push'></a>

This repository contains Cepheus which is part of the Giles Ecosystem. Cepheus is an app to extract images and embedded text from PDFs.

The Giles Ecosystem is a distributed system to run OCR on images and extract images and texts from PDF files. This repository contains the text and image extraction component of this system called "Cepheus". The system requires the following software:

* Apache Tomcat 8
* Apache Kafka
* Apache Zookeeper (required by Apache Kafka)
* Tesseract (https://github.com/tesseract-ocr/)

The components of the Giles Ecosystem are located in the following repositories:

* Giles: https://github.com/diging/giles-eco-giles-web (user-facing component for uploading files)
* Nepomuk: https://github.com/diging/giles-eco-nepomuk (file storage)
* Cepheus: https://github.com/diging/giles-eco-cepheus (this repository)
* Cassiopeia: https://github.com/diging/giles-eco-cassiopeia (OCR using Tesseract)

The above applications have dependencies to libraries located in the following repositories:

* https://github.com/diging/giles-eco-requests
* https://github.com/diging/giles-eco-util

There is a docker compose file for testing and evaluation purposes that sets up the Giles Ecosystem in Docker. You can find that file here: https://github.com/diging/giles-eco-docker
