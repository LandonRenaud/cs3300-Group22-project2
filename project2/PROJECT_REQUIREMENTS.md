# Project Requirements & Guidelines
Have all AI agents review this .md file for project requirements, rules, guidelines, etc.

## Project Summary
The topic we chose is meal prepping and grocery organizing. Our idea is to create a webapp/mobile application that will allow users to enter what foods/ingredients they buy when grocery shopping and have the app keep track of data associated with these products to create weekly meal plans.  

## Minimum Marketable Features
1. Ingredient Tracker - 
This feature will take a series of ingredients that the user provides, and the app will store data such as nutrition data and the quantity of the item that the user currently has.  

2. Weekly Meal Plan Provider - 
Using the ingredient tracker feature, the app will combine all of these and create a series of meals that the user can eat for a week. 

3. Expiry Date Tracker - 
One part of buying ingredients for yourself that can be annoying at times is finding out an ingredient or food has expired. This feature will associate each ingredient with an expiration date and display visual information to the user on how close each item is to expiration. This will also be taken into account for the weekly meal plan feature.  

4. Meal Storing Feature - 
In addition to ingredients, the app will also be able to store previously made meals and serve as a quick reference to the user when doing things like buying groceries and just needs to eyeball the ingredients used to make those meals.

## Design patterns
This project must implement 3 design patterns. The team has designated 3 design patterns which should be consisten across all areas of the code base

1. Singleton - database objects, and other API entry-point style objects shall be implemented in singleton pattern

2. Factory - Many of the features produce copies of information which can be represented as ojects, which might include things like foods/ingredients, meal plans, recipies, and more. The creation of new objects should be managed by factory classes which produce these

3. Strategy pattern - Since this app supports meal plan generation, there can be multiple different algorithms for generating different types of meal plans, such as prioritizing expiring ingredients, selecting meals based on nutritional info, etc.

## Testing
All features must have junit tests associated with them

## Other Info
- API keys and other sensitive information must be hidden from the front-end of the website
- Users must sign up and sign in with an email address and password before using the application
- Ingredient inventory and saved meals must belong to the signed-in user and remain available after an application restart
- Use the Firebase services associated with the Google Cloud project hosting the website for authentication and persistent storage
