package com.example.tysmp

enum class Answer {
    A, B
}

data class Quiz(
    val title: String,
    val question: String,
    val choiceA: String,
    val choiceB: String,
    val correctAnswer: Answer
) {
    companion object {
        fun createPlayNextQuiz(
        ): Quiz {
            val randomValue = (0..1).random()
            val correctChoice = "はい"
            val wrongChoice = "いいえ"
            val correctAnswer = if (randomValue == 1) Answer.A else Answer.B
            val choiceA = if (correctAnswer == Answer.A) correctChoice else wrongChoice
            val choiceB = if (correctAnswer == Answer.B) correctChoice else wrongChoice
            return Quiz(
                title = "つぎ の うた を ながしますか？",
                question = "つぎ の うた を ながしたい ひと は はい をおしてね♪",
                choiceA = choiceA,
                choiceB = choiceB,
                correctAnswer = correctAnswer
            )
        }

        fun createStopPlayingQuiz(
        ): Quiz {
            val randomValue = (0..1).random()
            val correctChoice = "はい"
            val wrongChoice = "いいえ"
            val correctAnswer = if (randomValue == 1) Answer.A else Answer.B
            val choiceA = if (correctAnswer == Answer.A) correctChoice else wrongChoice
            val choiceB = if (correctAnswer == Answer.B) correctChoice else wrongChoice
            return Quiz(
                title = "うた を とめますか？",
                question = "うた を とめたい ひと は はい をおしてね♪",
                choiceA = choiceA,
                choiceB = choiceB,
                correctAnswer = correctAnswer
            )
        }
    }
}
