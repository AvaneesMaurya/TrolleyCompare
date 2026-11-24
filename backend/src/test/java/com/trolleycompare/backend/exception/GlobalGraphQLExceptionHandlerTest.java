package com.trolleycompare.backend.exception;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GlobalGraphQLExceptionHandlerTest {

    @Test
    void mapsInvalidArgumentsToBadRequestErrors() {
        GraphQLError error = new TestableHandler().resolve(
                new IllegalArgumentException("Invalid quantity."), environment());

        assertEquals(ErrorType.BAD_REQUEST, error.getErrorType());
        assertEquals("Invalid Request: Invalid quantity.", error.getMessage());
    }

    @Test
    void mapsUnexpectedErrorsToInternalErrors() {
        GraphQLError error = new TestableHandler().resolve(
                new IllegalStateException("Database unavailable."), environment());

        assertEquals(ErrorType.INTERNAL_ERROR, error.getErrorType());
        assertEquals("TrolleyCompare Backend Error: Database unavailable.", error.getMessage());
    }

    private DataFetchingEnvironment environment() {
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
        Field field = mock(Field.class);
        when(stepInfo.getPath()).thenReturn(ResultPath.rootPath().segment("compareBasket"));
        when(field.getSourceLocation()).thenReturn(new SourceLocation(1, 1));
        when(environment.getExecutionStepInfo()).thenReturn(stepInfo);
        when(environment.getField()).thenReturn(field);
        return environment;
    }

    private static class TestableHandler extends GlobalGraphQLExceptionHandler {
        GraphQLError resolve(Throwable error, DataFetchingEnvironment environment) {
            return resolveToSingleError(error, environment);
        }
    }
}
