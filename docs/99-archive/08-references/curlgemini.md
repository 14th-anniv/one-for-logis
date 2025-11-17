 ``` curl 
"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent" 
\
  -H "x-goog-api-key: AIzaSyAKdlALe6BM84P4q3hqrKR7G24_Qfksvg4" \
  -H 'Content-Type: application/json' \
  -X POST \
  -d '{
    "contents": [
      {
        "parts": [
          {
            "text": "Explain how AI works in a few words"
          }
        ]
      }
    ]
  }'
 
  ⎿ {
      "candidates": [
        {
          "content": {
            "parts": [
              {
                "text": "AI learns patterns from data to make decisions or predictions, 
mimicking
     human intelligence."
              }
            ],
            "role": "model"
          },
          "finishReason": "STOP",
          "index": 0
        }
      ],
      "usageMetadata": {
        "promptTokenCount": 8,
        "candidatesTokenCount": 15,
        "totalTokenCount": 933,
        "promptTokensDetails": [
          {
            "modality": "TEXT",
            "tokenCount": 8
          }
        ],
        "thoughtsTokenCount": 910
      },
      "modelVersion": "gemini-2.5-flash",
      "responseId": "-hMMaefvHtz-2roP8YWIyQo"
    }
      % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                     Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0
100   156    0     0  100 0      0 --:--:-- --:--:-- --:--:--     0
100   156    0     0  100   156      01 --:--:--   123
100   156    0     0  100   156      0     47  0:00:03
100   156    0     0  100   156      0     36  0:00:04  0:00:04
100   156    0     0  100   156      0     29  0:00:05  0:00:05 --:--:--
100   790    0   634  100   156    111     27  0:00:05  0:00:05 --:--:--   143``` 
