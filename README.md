# graphql-transformer

Java tool to transform GraphQL requests. 

Useful in services acting as facades / proxies in front of a graphQL service.


* Add requested fields to query / mutation response (and remove corresponding fields from response)
* Add arguments
* Manipulate arguments


## Usage


### Add selection field to request

        // Parse GraphQLRequest from String payload
        GraphQLRequest graphQLRequest = new GraphQLRequest(payload, objectMapper);

        // Add selection field to request if not already exists, keep path for added field
        List<String> addedPath = graphQLRequest.addSelectionField("operationName", "parentField1", "parentField2", "newField");
        
        // Convert request to string and send to GraphQL service
        String responsePayload = client.sendRequest(graphQLRequest.writeValueAsString());
        
        // Parse response to JsonNode
        ObjectNode jsonResponse=objectMapper.readValue(responsePayload, ObjectNode.class);
        
        // Remove selectionField if added
        JsonNodeUtil.removeField(jsonResponse, addedPath);



### Manipulate argument values

        GraphQLRequest graphQLRequest = new GraphQLRequest(payload, objectMapper);

        // Read argument value
        ArgumentValue argumentValue = graphQLRequest.getArgumentValue("fieldName", "parentField1", "parentField2", "newField");

        // Create or update argument
        graphQLRequest.setArgumentValue("fieldName", "argumentName", new StringValue("argumentValue"));

        // Return to string representation
        String transformedPayload = graphQLRequest.writeValueAsString();

         