package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GraphqlExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleResourceNotFound(ResourceNotFoundException ex, DataFetchingEnvironment environment) {
        return GraphqlErrorBuilder.newError()
            .errorType(ErrorType.NOT_FOUND)
            .message(ex.getMessage())
            .path(environment.getExecutionStepInfo().getPath())
            .location(environment.getField().getSourceLocation())
            .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleGeneralException(RuntimeException ex, DataFetchingEnvironment environment) {
        return GraphqlErrorBuilder.newError()
            .errorType(ErrorType.INTERNAL_ERROR)
            .message(ex.getMessage() != null ? ex.getMessage() : "Unknown internal error")
            .path(environment.getExecutionStepInfo().getPath())
            .location(environment.getField().getSourceLocation())
            .build();
    }
}