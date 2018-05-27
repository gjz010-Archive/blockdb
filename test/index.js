const grpc=require('grpc');
const PROTO_PATH="../src/main/proto/db.proto";
const db=grpc.load(PROTO_PATH);
const config=require('../config.json');
const bluebird=require('bluebird');
bluebird.promisifyAll(db.blockdb.BlockDatabase.prototype);
const stub=new db.blockdb.BlockDatabase(config["1"]["ip"]+":"+config["1"]["port"], grpc.credentials.createInsecure());
//Function to start a server that does not crash.
async function startStable(){


}
//Function to start a server that often crashes.
async function startUnstable(){

}
const client={
	"LogLength": async ()=>{
		return (await stub.logLengthAsync({})).Value;
	},
	"get": async(user)=>{
		return (await stub.getAsync({"UserID": user})).Value;
	},
	"put": async(user, val)=>{
		return (await stub.putAsync({"UserID": user, "Value": val})).Success;
	},
	"deposit": async(user, val)=>{
		return (await stub.depositAsync({"UserID": user, "Value": val})).Success;
	},
	"withdraw": async(user, val)=>{
		return (await stub.withdrawAsync({"UserID": user, "Value": val})).Success;
		
	},
	"transfer": async(ufrom, uto, val)=>{
		return (await stub.transferAsync({"FromID": ufrom, "ToID": uto, "Value": val})).Success;
	}
	
	
}

async function test(){
	for(let i=0;i<50;i++){
		await client.deposit("gjz010",10);
	}
	return await client.get("gjz010");
	
}
test().then(console.log).catch(console.log);