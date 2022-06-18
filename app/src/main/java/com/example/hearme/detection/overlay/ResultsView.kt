package com.example.hearme.detection.overlay

import com.example.hearme.detection.tf.Detector.Recognition

interface ResultsView {
    fun setResults(results: List<Recognition?>?)
}