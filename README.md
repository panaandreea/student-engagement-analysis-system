# Project Structure

This repository is structured into the following main components:

## android-client
Android application designed to support teachers in:
- scheduling and managing attention monitoring sessions
- visualizing attention metrics in real time
- analyzing the evolution of each student's attention throughout a session

## ml-model
Module dedicated to the development and training of the machine learning model, including:
- data preprocessing
- model training, evaluation, and validation

## ml-service
Background service responsible for:
- real-time data acquisition and processing
- tracking attention at the individual student level within a session
- performing predictions using the integrated machine learning model
- storing prediction results in the database

## thesis
Bachelor’s thesis presenting:
- system design and architecture
- implementation and methodology
- evaluation and results