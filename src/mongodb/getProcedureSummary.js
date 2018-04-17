db.models.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			  "＄domain": "Model.FPMN.Diagram"
			}
		},

		// Stage 2
		{
			$project: {
			    "diagram": "$$ROOT"
			}
		},

		// Stage 3
		{
			$lookup: {
			    from: "diagrams",
			    localField: "diagram.planeId",
			    foreignField: "_id",
			    as: "procedure"
			}
		},

		// Stage 4
		{
			$unwind: "$procedure"
		},

		// Stage 5
		{
			$lookup: {
			    from: "models",
			    localField: "procedure.modelId",
			    foreignField: "_id",
			    as: "procedure"
			}
		},

		// Stage 6
		{
			$match: {
			  // {procedureId}
			  "procedure.＄domain": "Model.FPMN.Procedure"
			}
		},

		// Stage 7
		{
			$unwind: "$procedure"
		},

		// Stage 8
		{
			$graphLookup: {
			    "from": "models",
			    "startWith": "$procedure._id",
			    "connectFromField": "_id",
			    "connectToField": "parentId",
			    "as": "phases",
			    "maxDepth": 0,
			    "restrictSearchWithMatch": {
			        "nextPhaseId": null,
			        "＄domain": "Model.FPMN.Phase"
			    }
			}
		},

		// Stage 9
		{
			$graphLookup: {
			      "from": "models",
			      "startWith": "$phases._id",
			      "connectFromField": "prevPhaseId",
			      "connectToField": "_id",
			      "as": "phases"
			}
		},

		// Stage 10
		{
			$unwind: "$phases"
		},

		// Stage 11
		{
			$graphLookup: {
			      "from": "models",
			      "startWith": "$phases._id",
			      "connectFromField": "_id",
			      "connectToField": "parentId",
			      "as": "eServices",
			      "restrictSearchWithMatch": {
			        "eServiceId": {
			          "$exists": true
			        }
			      }
			}
		},

		// Stage 12
		{
			$addFields: {
			    "phases.eServiceIds": "$eServices.eServiceId"
			}
		},

		// Stage 13
		{
			$group: {
			      "_id": "$_id",
			      "diagram": {
			        "$first": "$diagram"
			      },
			      "procedure": {
			        "$first": "$procedure"
			      },
			      "phases": {
			        "$push": "$phases"
			      }
			}
		},

		// Stage 14
		{
			$match: {
			    "phases.eServiceIds": ["08677aef-2b6d-4fff-bf10-91b2fe82bdc5"] // {{eServiceId}}
			}
		},

		// Stage 15
		{
			$addFields: {
			    "phases": {
			        "$map": {
			          "input": "$phases",
			          "as": "phase",
			          "in": {
			            "_id": "$$phase._id",
			            "eServiceIds": "$$phase.eServiceIds",
			            "language": "$$phase.language",
			            "name": "$$phase.name",
			            "documentation": "$$phase.documentation",
			            "url": {
			              "$concat": [
			                "{appDiagramUrl}",
			                "$diagram._id",
			                "/",
			                "$$phase._id"
			              ]
			            }
			          }
			        }
			      }
			}
		},

		// Stage 16
		{
			$project: {
			      "_id": "$procedure._id",
			      "notation": "$diagram.notation",
			      "version": "$diagram.version",
			      "created": "$diagram.created",
			      "lastModified": "$diagram.lastModified",
			      "language": "$procedure.language",
			      "name": "$procedure.name",
			      "documentation": "$procedure.documentation",
			      "mission": "$procedure.mission",
			      "regulatoryRefs": "$procedure.regulatoryRefs",
			      "phases": "$phases",
			      "url": {
			        "$concat": [
			          "{appDiagramUrl}",
			          "$diagram._id",
			          "/",
			          "$procedure._id"
			        ]
			      },
			      "svg": {
			        "$concat": [
			          "{appDiagramSvg}",
			          "$diagram._id",
			          ".svg"
			        ]
			      }
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
