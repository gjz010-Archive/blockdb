const grpc=require('grpc');
const PROTO_PATH="../src/main/proto/db.proto";
const db=grpc.load(PROTO_PATH);
const config=require('../config.json');
const bluebird=require('bluebird');
const assert=require('assert');
bluebird.promisifyAll(db.blockdb.BlockDatabase.prototype);
const stub=new db.blockdb.BlockDatabase("127.0.0.1"+":"+config["1"]["port"], grpc.credentials.createInsecure(),
{
  'grpc.min_reconnect_backoff_ms': 1000,
  'grpc.max_reconnect_backoff_ms': 1000,
}
);
//Function to start a server that does not crash.
async function startStable(){


}
//Function to start a server that often crashes.
async function startUnstable(){

}
const client={
	"LogLength": async ()=>{
		try{
			return (await stub.logLengthAsync({})).Value;
		}catch(err){
            //console.log(err)
			console.log("Error caught in LogLength! Retrying...");
			return await client.LogLength();
		}
	},
	"LOG": async()=>{return await client.LogLength();},
	"GET": async(user)=>{
		try{
			return (await stub.getAsync({"UserID": user})).Value;
		}catch(err){
			console.log("Error caught in GET! Retrying...");
			return await client.GET(user);
			
		}
	},
	"PUT": async(user, val)=>{
		const last=await client.LogLength();
		try{
			return (await stub.putAsync({"UserID": user, "Value": Number.parseInt(val)})).Success;
		}catch(err){
			console.log("Error caught in PUT!");
			const curr=await client.LogLength();
			if(last==curr){
				console.log("Retrying...");
				return await client.PUT(user, val);
			}else{
				console.log("Log added. Not retrying.");
				return true;
			}
		}
	},
	"DEPOSIT": async(user, val)=>{
		const last=await client.LogLength();
		try{
			return (await stub.depositAsync({"UserID": user, "Value": Number.parseInt(val)})).Success;
		}catch(err){
			console.log("Error caught in DEPOSIT!");
			const curr=await client.LogLength();
			if(last==curr){
				console.log("Retrying...");
				return await client.DEPOSIT(user, val);
			}else{
				console.log("Log added. Not retrying.");
				return true;
			}
		}
		
	},
	"WITHDRAW": async(user, val)=>{
		const last=await client.LogLength();
		try{
			return (await stub.withdrawAsync({"UserID": user, "Value": Number.parseInt(val)})).Success;
		}catch(err){
			console.log("Error caught in WITHDRAW!");
			const curr=await client.LogLength();
			if(last==curr){
				console.log("Retrying...");
				return await client.WITHDRAW(user, val);
			}else{
				console.log("Log added. Not retrying.");
				return true;
			}
		}
		
		
	},
	"TRANSFER": async(ufrom, uto, val)=>{
		const last=await client.LogLength();
		try{
			return (await stub.transferAsync({"FromID": ufrom, "ToID": uto, "Value": Number.parseInt(val)})).Success;
		}catch(err){
			console.log("Error caught in TRANSFER!");
			const curr=await client.LogLength();
			if(last==curr){
				console.log("Retrying...");
				return await client.TRANSFER(ufrom, uto, val);
			}else{
				console.log("Log added. Not retrying.");
				return true;
			}
		}
		
	}
	
}
const readline=require('readline')

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
})
const cmd_param_count={
	"GET":1, "PUT": 2, "LOG":0, "WITHDRAW": 2, "DEPOSIT": 2, "TRANSFER": 3
	
}
function readCommand() {
	return new Promise((resolve, reject) => {
		rl.question('db>', async (command) => {
			command=command.split(" ");
			command[0]=command[0].toUpperCase();
			if(client[command[0]]){
				if(cmd_param_count[command[0]]+1!=command.length){
					reject(`Bad arguments. ${cmd_param_count[command[0]]} required, but found ${command.length-1}.`);
					
				}else{
					const val=await client[command[0]](command[1], command[2], command[3]);
					if(command[0]=="GET"){
						console.log(`${command[1]} = ${val}`);
						resolve(val);
					}else{
						console.log(val);
						resolve(val);
					}
					
					
				}
				
			}else{
				reject("Bad Command.");
				
			}
			
		});
	});
}


async function repl(){
	while(true){
		try{
			(await readCommand());
		}catch(err){
			console.log("Error: "+err);
		}
		
	}
	
}
repl();
