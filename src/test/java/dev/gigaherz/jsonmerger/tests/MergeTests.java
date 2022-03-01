package dev.gigaherz.jsonmerger.tests;

import com.google.gson.JsonParser;
import dev.gigaherz.jsonmerger.JsonMerger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MergeTests
{
    @Test
    public void singularStackReturnsSingleElement()
    {
        var a = JsonParser.parseString("""
                {"a":"b"}
                """);

        var result = JsonMerger.combineStack(List.of(a));

        Assertions.assertEquals(a, result);
    }

    @Test
    public void defaultStackReturnsLast()
    {
        var a = JsonParser.parseString("""
                {"a":"b"}
                """);
        var b = JsonParser.parseString("""
                {"c":"d"}
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertSame(b, result);
    }

    @Test
    public void objectOverwriteReturnsSecond()
    {
        var a = JsonParser.parseString("""
                {
                    "a":"b"
                }
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode":"overwrite"
                    },
                    
                    "c":"d"
                }
                """);
        var c = JsonParser.parseString("""
                {
                    "c":"d"
                }
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void objectCombineReturnsCombined()
    {
        var a = JsonParser.parseString("""
                {
                    "a":"b"
                }
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode":"combine"
                    },
                    
                    "c":"d"
                }
                """);
        var c = JsonParser.parseString("""
                {
                    "a":"b",
                    "c":"d"
                }
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void objectCombineCombinesChildObject()
    {
        var a = JsonParser.parseString("""
                {
                    "a": {
                        "b": "c"
                    }
                }
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode":"combine"
                    },
                    
                    "a": {
                        "d": "e"
                    }
                }
                """);
        var c = JsonParser.parseString("""
                {
                    "a":{
                        "b": "c",
                        "d": "e"
                    }
                }
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void objectCombineCombinesChildArray()
    {
        var a = JsonParser.parseString("""
                {
                    "a": [
                        "b", "c"
                    ]
                }
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode":"combine"
                    },
                    
                    "a": [
                        "d", "e"
                    ]
                }
                """);
        var c = JsonParser.parseString("""
                {
                    "a":[
                        "b", "c",
                        "d", "e"
                    ]
                }
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void arrayCombineReturnsCombined()
    {
        var a = JsonParser.parseString("""
                [ "a", "b" ]
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode":"combine",
                        "value":[ "c", "d" ]
                    }
                }
                """);
        var c = JsonParser.parseString("""
                [
                    "a", "b",
                    "c", "d"
                ]
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void arrayCombineConcatenatesArrays()
    {
        var a = JsonParser.parseString("""
                [
                    {"a":"b"}
                ]
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode":"combine",
                        "value":[
                            {"c":"d"}
                        ]
                    }
                }
                """);
        var c = JsonParser.parseString("""
                [
                    {
                        "a":"b"
                    },
                    {
                        "c":"d"
                    }
                ]
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void arrayZipCombinesChildObject()
    {
        var a = JsonParser.parseString("""
                [
                    {"a":"b"}
                ]
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode": "zip",
                        "value":[
                            {"c":"d"}
                        ]
                    }
                }
                """);
        var c = JsonParser.parseString("""
                [
                    {
                        "a": "b",
                        "c": "d"
                    }
                ]
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void childOfArrayInsertInsertsElement()
    {
        var a = JsonParser.parseString("""
                [
                    "a","b"
                ]
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode": "combine",
                        "value": [
                            {
                            
                                "_jm_combine": {
                                    "mode": "insert",
                                    "index": 1
                                },
                                "c": "d"
                            }
                        ]
                    }
                }
                """);
        var c = JsonParser.parseString("""
                [
                    "a",
                    {
                        "c":"d"
                    },
                    "b"
                ]
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void childOfArrayInsertInsertsValue()
    {
        var a = JsonParser.parseString("""
                [
                    "a","b"
                ]
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode": "combine",
                        "value": [
                            {
                                "_jm_combine": {
                                    "mode": "insert",
                                    "index": 1,
                                    "value":[
                                        "c","d"
                                    ]
                                }
                            }
                        ]
                    }
                }
                """);
        var c = JsonParser.parseString("""
                [
                    "a",
                    [
                        "c","d"
                    ],
                    "b"
                ]
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void childOfArrayOverwriteReplacesElement()
    {
        var a = JsonParser.parseString("""
                [
                    "a","b","c"
                ]
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode": "combine",
                        "value": [
                            {
                            
                                "_jm_combine": {
                                    "mode": "overwrite",
                                    "index": 1
                                },
                                "d": "e"
                            }
                        ]
                    }
                }
                """);
        var c = JsonParser.parseString("""
                [
                    "a",
                    {
                        "d":"e"
                    },
                    "c"
                ]
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void childOfArrayOverwriteReplacesValue()
    {
        var a = JsonParser.parseString("""
                [
                    "a","b","c"
                ]
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode": "combine",
                        "value": [
                            {
                            
                                "_jm_combine": {
                                    "mode": "overwrite",
                                    "index": 1,
                                    "value":[
                                        "d","e"
                                    ]
                                }
                                
                            }
                        ]
                    }
                }
                """);
        var c = JsonParser.parseString("""
                [
                    "a",
                    [
                        "d", "e"
                    ],
                    "c"
                ]
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }

    @Test
    public void childOfArrayDeleteRemovesEntry()
    {
        var a = JsonParser.parseString("""
                [
                    "a","b","c"
                ]
                """);
        var b = JsonParser.parseString("""
                {
                    "_jm_combine": {
                        "mode": "combine",
                        "value": [
                            {
                            
                                "_jm_combine": {
                                    "mode": "delete",
                                    "index": 1
                                }
                            }
                        ]
                    }
                }
                """);
        var c = JsonParser.parseString("""
                [
                    "a",
                    "c"
                ]
                """);

        var result = JsonMerger.combineStack(List.of(a, b));

        Assertions.assertEquals(c, result);
    }
}
