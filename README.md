# Explain Method Plugin for IntelliJ IDEA

This IntelliJ IDEA plugin allows you to explain Python methods 
in plain English using ChatGPT. It provides a concise summary of 
the method and identifies external references, 
making it easier to understand and document your code.

## Features

- **Method Explanation**: Get a plain English explanation of the selected Python method.
- **External References**: Identify and display external references used in the method.
- **Code Compression**: Compact the method for a more concise overview.

## Installation

1. Clone the repository.
2. Open the project in IntelliJ IDEA.
3. **EXTREMELY IMPORTANT**: in the `resources/META-INF/user_data.json` file you need to add your OpenAI API key. 
   You can get one [here](https://beta.openai.com/). They key must be added between the quotes in the `api_key` field.
4. Run the `runIde` Gradle task, or Run Plugin from the Run menu.

## Usage

1. When you run the project a PyCharm VM IDE will run as well.
2. Create a new Python Project or open an existing one.
3. Select the method you want to explain.
4. Right-click and choose `Explain This Method!` from the context menu.
5. The explanation will be displayed in a popup.
6. **EXTREMELY IMPORTANT**: Since there is asynchronicity involved, you need to wait for the popup to appear, while you 
can still work on your project. The popup will appear in a couple of seconds in the middle of the screen with the 
explanation.

## Configuration

Besides the API key, which can be configured in the `resources/META-INF/user_data.json` file, there are no other.

## Acknowledgments

Thanks to the [ChatGPT](https://www.openai.com/gpt) API for providing natural language processing capabilities.

## Contact

If you have any questions or issues, feel free to [open an issue](https://github.com/your-username/your-project/issues).

